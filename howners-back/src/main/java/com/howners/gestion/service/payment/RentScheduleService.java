package com.howners.gestion.service.payment;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génération automatique des échéances de loyer : le bailleur n'a plus à créer chaque
 * loyer à la main. L'échéance RENT du mois courant est créée (a) à l'activation du bail
 * et (b) par un rattrapage quotidien idempotent pour tous les baux ACTIFS (survit aux
 * redémarrages : pas de « mois sauté » si l'instance était éteinte le 1er).
 *
 * Règles : montant = loyer + charges, proratisé au nombre de jours couverts pour les mois
 * partiels (entrée ou sortie en cours de mois) ; échéance au jour de paiement du bail
 * (payment_day, défaut le 1er), jamais avant le début d'occupation ni dans le passé.
 * Une échéance annulée par le bailleur n'est PAS recréée (il garde la main).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentScheduleService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /** Rattrapage quotidien : garantit l'échéance du mois courant pour chaque bail actif. */
    @Scheduled(cron = "0 45 0 * * ?")
    @Transactional
    public void generateMonthlyRents() {
        List<Rental> actives = rentalRepository.findByStatus(RentalStatus.ACTIVE);
        int created = 0;
        for (Rental rental : actives) {
            try {
                if (ensureRentPayment(rental, YearMonth.now())) {
                    created++;
                }
            } catch (Exception e) {
                log.error("Échec de génération du loyer pour le bail {}: {}", rental.getId(), e.getMessage(), e);
            }
        }
        if (created > 0) {
            log.info("{} échéance(s) de loyer générée(s) automatiquement", created);
        }
    }

    /**
     * Crée l'échéance de loyer du mois demandé si elle n'existe pas déjà.
     * Idempotent ; retourne true si une échéance a été créée.
     */
    @Transactional
    public boolean ensureRentPayment(Rental rental, YearMonth month) {
        if (rental == null || rental.getTenant() == null) return false;
        if (rental.getMonthlyRent() == null || rental.getMonthlyRent().signum() <= 0) return false;

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        // Jours réellement couverts par le bail sur ce mois (entrée/sortie en cours de mois).
        LocalDate coverStart = rental.getStartDate() != null && rental.getStartDate().isAfter(monthStart)
                ? rental.getStartDate() : monthStart;
        LocalDate coverEnd = rental.getEndDate() != null && rental.getEndDate().isBefore(monthEnd)
                ? rental.getEndDate() : monthEnd;
        if (coverStart.isAfter(coverEnd)) return false; // bail hors de ce mois

        // Tout paiement RENT déjà posé sur le mois (même annulé) bloque la génération.
        if (paymentRepository.existsAnyRentPaymentInMonth(rental.getId(), monthStart, monthStart.plusMonths(1))) {
            return false;
        }

        BigDecimal fullAmount = rental.getMonthlyRent()
                .add(rental.getCharges() != null ? rental.getCharges() : BigDecimal.ZERO);
        long coveredDays = coverEnd.toEpochDay() - coverStart.toEpochDay() + 1;
        BigDecimal amount = coveredDays == month.lengthOfMonth()
                ? fullAmount
                : fullAmount.multiply(BigDecimal.valueOf(coveredDays))
                        .divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);

        // Échéance au jour de paiement du bail, bornée au mois, jamais avant le début
        // d'occupation ni dans le passé (activation tardive → exigible immédiatement).
        int payDay = rental.getPaymentDay() != null && rental.getPaymentDay() >= 1 && rental.getPaymentDay() <= 31
                ? Math.min(rental.getPaymentDay(), month.lengthOfMonth()) : 1;
        LocalDate dueDate = month.atDay(payDay);
        if (dueDate.isBefore(coverStart)) dueDate = coverStart;
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today) && !today.isAfter(monthEnd)) dueDate = today;

        Payment payment = Payment.builder()
                .rental(rental)
                .payer(rental.getTenant())
                .paymentType(PaymentType.RENT)
                .amount(amount)
                .currency(rental.getCurrency() != null ? rental.getCurrency() : "EUR")
                .status(PaymentStatus.PENDING)
                .dueDate(dueDate)
                .build();
        payment = paymentRepository.save(payment);
        log.info("Loyer {} généré automatiquement pour le bail {} (échéance {})",
                amount, rental.getId(), dueDate);

        notifyTenant(payment, coveredDays < month.lengthOfMonth());
        return true;
    }

    /** Notification in-app + e-mail au locataire (best-effort, ne bloque jamais la génération). */
    private void notifyTenant(Payment payment, boolean prorated) {
        try {
            Rental rental = payment.getRental();
            User tenant = payment.getPayer();
            String property = rental.getProperty() != null ? rental.getProperty().getName() : "votre location";
            String amountLabel = payment.getAmount() + " " + payment.getCurrency();
            String dueLabel = payment.getDueDate() != null ? payment.getDueDate().format(FR_DATE) : "";
            String prorataNote = prorated ? " (montant proratisé au nombre de jours d'occupation du mois)" : "";

            notificationDispatcher.notifyAndEmail(tenant, NotificationType.PAYMENT_DUE,
                    "Votre loyer est disponible",
                    "Le loyer de " + amountLabel + " pour " + property + " est à régler avant le "
                            + dueLabel + prorataNote + ".",
                    "/payments/" + payment.getId(),
                    new NotificationDispatcher.Email(
                            "Votre loyer — " + property,
                            "Échéance de loyer",
                            "Votre loyer de <strong>" + amountLabel + "</strong> pour <strong>" + property
                                    + "</strong> est à régler avant le " + dueLabel + prorataNote
                                    + ". Retrouvez les moyens de paiement dans votre espace.",
                            null,
                            "Voir mon échéance",
                            frontendUrl + "/payments/" + payment.getId(),
                            false));
        } catch (Exception e) {
            log.error("Échec de la notification de loyer généré {}: {}", payment.getId(), e.getMessage());
        }
    }
}
