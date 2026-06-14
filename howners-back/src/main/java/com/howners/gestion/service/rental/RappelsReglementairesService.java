package com.howners.gestion.service.rental;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Rappels d'échéances réglementaires (quotidien, best-effort) :
 * - Fin de bail : notification owner à J-180, J-90 et J-30 avant rental.endDate.
 * - DPE : validité 10 ans — alerte à J-90, J-30 et à expiration.
 * - Assurance habitation locataire : alerte à J-30 et à échéance dépassée (jour J).
 * Les fenêtres sont des dates exactes : si le job ne tourne pas un jour donné,
 * le rappel de cette fenêtre est perdu (assumé en V1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RappelsReglementairesService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<Integer> FENETRES_FIN_BAIL = List.of(180, 90, 30);
    private static final int VALIDITE_DPE_ANNEES = 10;

    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(cron = "0 15 7 * * ?")
    @Transactional
    public void verifierEcheances() {
        rappelsFinDeBail();
        rappelsDpe();
        rappelsAssurance();
    }

    private void rappelsFinDeBail() {
        for (int jours : FENETRES_FIN_BAIL) {
            LocalDate cible = LocalDate.now().plusDays(jours);
            for (Rental rental : rentalRepository.findActiveRentalsEndingOn(cible)) {
                User owner = rental.getProperty().getOwner();
                notificationService.create(
                        owner.getId(),
                        NotificationType.LEASE_END,
                        "Fin de bail dans " + jours + " jours",
                        String.format("Le bail de %s se termine le %s. Pensez au préavis, au renouvellement ou à la remise en location.",
                                rental.getProperty().getName(), cible.format(FR_DATE)),
                        "/rentals/" + rental.getId());
                log.info("Rappel fin de bail J-{} pour la location {}", jours, rental.getId());
            }
        }
    }

    private void rappelsDpe() {
        // dpeDate + 10 ans dans moins de 90 jours (ou déjà dépassé)
        LocalDate seuilDpeDate = LocalDate.now().plusDays(90).minusYears(VALIDITE_DPE_ANNEES);
        for (Property property : propertyRepository.findByDpeDateNotNullAndDpeDateBefore(seuilDpeDate)) {
            LocalDate expiration = property.getDpeDate().plusYears(VALIDITE_DPE_ANNEES);
            long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiration);
            // N'alerter qu'aux fenêtres J-90, J-30 et le jour de l'expiration (anti-spam)
            boolean fenetre = joursRestants == 90 || joursRestants == 30 || joursRestants == 0;
            if (!fenetre) continue;

            notificationService.create(
                    property.getOwner().getId(),
                    NotificationType.DPE_EXPIRY,
                    expiration.isAfter(LocalDate.now()) ? "DPE bientôt expiré" : "DPE expiré",
                    String.format("Le DPE de %s %s le %s. Un DPE valide est obligatoire pour louer ou vendre.",
                            property.getName(),
                            expiration.isAfter(LocalDate.now()) ? "expire" : "a expiré",
                            expiration.format(FR_DATE)),
                    "/properties/" + property.getId());
            log.info("Rappel DPE pour le bien {} (expiration {})", property.getId(), expiration);
        }
    }

    private void rappelsAssurance() {
        LocalDate dansTrenteJours = LocalDate.now().plusDays(30);
        for (Rental rental : rentalRepository.findActiveWithAssuranceExpiringBefore(dansTrenteJours)) {
            LocalDate echeance = rental.getAssuranceExpiration();
            boolean fenetre = echeance.equals(dansTrenteJours) || echeance.equals(LocalDate.now());
            if (!fenetre) continue;

            User owner = rental.getProperty().getOwner();
            User tenant = rental.getTenant();
            boolean expiree = !echeance.isAfter(LocalDate.now());

            notificationService.create(
                    owner.getId(),
                    NotificationType.INSURANCE_RENEWAL,
                    expiree ? "Assurance habitation expirée" : "Assurance habitation à renouveler",
                    String.format("L'attestation d'assurance du locataire de %s %s le %s. Demandez la nouvelle attestation.",
                            rental.getProperty().getName(),
                            expiree ? "a expiré" : "expire",
                            echeance.format(FR_DATE)),
                    "/rentals/" + rental.getId());

            if (tenant != null && tenant.getEmail() != null) {
                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        "Votre attestation d'assurance habitation " + (expiree ? "a expiré" : "expire bientôt"),
                        "Assurance habitation",
                        String.format(
                                "L'attestation d'assurance habitation de votre logement (%s) %s le <strong>%s</strong>. "
                                        + "L'assurance du logement est une obligation légale du locataire : merci de transmettre "
                                        + "votre nouvelle attestation à votre bailleur.",
                                rental.getProperty().getName(),
                                expiree ? "a expiré" : "expire",
                                echeance.format(FR_DATE)),
                        null,
                        null,
                        null,
                        expiree
                ));
            }
            log.info("Rappel assurance pour la location {} (échéance {})", rental.getId(), echeance);
        }
    }
}
