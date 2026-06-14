package com.howners.gestion.service.payment;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.payment.PaymentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Relances d'impayés escaladées :
 * - Niveau 1 (J+5 après échéance) : email de relance au locataire.
 * - Niveau 2 (J+15) : mise en demeure PDF (art. 24, loi n° 89-462) archivée et envoyée.
 * Le job tourne à 8h30, après markOverduePayments (8h) qui marque les paiements LATE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelanceImpayesService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int NIVEAU_RELANCE = 1;
    private static final int NIVEAU_MISE_EN_DEMEURE = 2;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void traiterRelances() {
        LocalDate today = LocalDate.now();

        // Mise en demeure d'abord (J+15) pour ne pas envoyer relance + mise en demeure le même jour
        List<Payment> pourMiseEnDemeure = paymentRepository
                .findLatePaymentsForRelance(today.minusDays(15), NIVEAU_MISE_EN_DEMEURE).stream()
                .filter(p -> p.getRelanceNiveau() == NIVEAU_RELANCE)
                .toList();
        pourMiseEnDemeure.forEach(this::envoyerMiseEnDemeure);

        List<Payment> pourRelance = paymentRepository
                .findLatePaymentsForRelance(today.minusDays(5), NIVEAU_RELANCE).stream()
                .filter(p -> p.getRelanceNiveau() == 0)
                .toList();
        pourRelance.forEach(this::envoyerRelance);

        if (!pourRelance.isEmpty() || !pourMiseEnDemeure.isEmpty()) {
            log.info("Relances impayés : {} relances, {} mises en demeure",
                    pourRelance.size(), pourMiseEnDemeure.size());
        }
    }

    /**
     * Déclenchement manuel par le bailleur : passe immédiatement à l'étape suivante.
     */
    @Transactional
    public PaymentResponse relancerManuellement(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId.toString()));
        checkOwnerAccess(payment);

        if (payment.getStatus() != PaymentStatus.LATE && payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Seul un paiement en retard ou en attente peut être relancé.");
        }
        if (payment.getRelanceNiveau() >= NIVEAU_MISE_EN_DEMEURE) {
            throw new BadRequestException("La mise en demeure a déjà été envoyée pour ce paiement.");
        }

        if (payment.getRelanceNiveau() == 0) {
            envoyerRelance(payment);
        } else {
            envoyerMiseEnDemeure(payment);
        }
        return PaymentResponse.from(payment);
    }

    private void envoyerRelance(Payment payment) {
        Rental rental = payment.getRental();
        User tenant = payment.getPayer();
        User owner = rental.getProperty().getOwner();

        if (tenant != null && tenant.getEmail() != null) {
            emailService.sendNotificationEmail(new GenericNotificationEmailData(
                    tenant.getEmail(),
                    tenant.getFullName(),
                    "Relance — loyer impayé pour " + rental.getProperty().getName(),
                    "Loyer impayé — relance",
                    String.format(
                            "Sauf erreur de notre part, le paiement de <strong>%.2f €</strong> dû le <strong>%s</strong> "
                                    + "pour le logement situé %s n'a pas été reçu. Merci de régulariser votre situation dans les meilleurs délais.",
                            payment.getAmount(),
                            payment.getDueDate() != null ? payment.getDueDate().format(FR_DATE) : "—",
                            adresse(rental)),
                    null,
                    "Payer maintenant",
                    null,
                    true
            ));
        }

        if (tenant != null) {
            notificationService.create(tenant.getId(), NotificationType.PAYMENT_OVERDUE,
                    "Relance de paiement",
                    String.format("Le paiement de %.2f € est en retard. Merci de régulariser.", payment.getAmount()),
                    "/payments");
        }
        notificationService.create(owner.getId(), NotificationType.PAYMENT_OVERDUE,
                "Relance envoyée",
                String.format("Une relance a été envoyée à %s pour le paiement de %.2f € (%s).",
                        tenant != null ? tenant.getFullName() : "—", payment.getAmount(),
                        rental.getProperty().getName()),
                "/payments");

        payment.setRelanceNiveau(NIVEAU_RELANCE);
        payment.setDerniereRelanceLe(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Relance niveau 1 envoyée pour le paiement {}", payment.getId());
    }

    private void envoyerMiseEnDemeure(Payment payment) {
        Rental rental = payment.getRental();
        User tenant = payment.getPayer();
        User owner = rental.getProperty().getOwner();

        // Mise en demeure PDF
        String html = buildMiseEnDemeureHtml(payment);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdf(html, null);
        } catch (IOException e) {
            log.error("Échec de génération de la mise en demeure pour le paiement {} : {}",
                    payment.getId(), e.getMessage());
            return;
        }

        String fileName = String.format("mise_en_demeure_%s_%d.pdf", payment.getId(), System.currentTimeMillis());
        String fileKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

        Document document = Document.builder()
                .rental(rental)
                .property(rental.getProperty())
                .uploader(owner)
                .documentType(DocumentType.MISE_EN_DEMEURE)
                .fileName(fileName)
                .filePath(fileKey)
                .fileKey(fileKey)
                .fileSize((long) pdfBytes.length)
                .mimeType("application/pdf")
                .documentHash(pdfService.calculateHash(pdfBytes))
                .description(String.format("Mise en demeure — paiement de %.2f € dû le %s",
                        payment.getAmount(),
                        payment.getDueDate() != null ? payment.getDueDate().format(FR_DATE) : "—"))
                .build();
        documentRepository.save(document);

        String documentUrl = storageService.generatePresignedUrl(fileKey);

        if (tenant != null && tenant.getEmail() != null) {
            emailService.sendNotificationEmail(new GenericNotificationEmailData(
                    tenant.getEmail(),
                    tenant.getFullName(),
                    "MISE EN DEMEURE — loyer impayé pour " + rental.getProperty().getName(),
                    "Mise en demeure de payer",
                    String.format(
                            "Malgré notre relance, le paiement de <strong>%.2f €</strong> dû le <strong>%s</strong> demeure impayé. "
                                    + "Vous êtes mis(e) en demeure de régler cette somme sous <strong>8 jours</strong>. "
                                    + "À défaut, le bailleur pourra engager les démarches prévues par votre bail et par la loi "
                                    + "(article 24 de la loi n° 89-462 du 6 juillet 1989).",
                            payment.getAmount(),
                            payment.getDueDate() != null ? payment.getDueDate().format(FR_DATE) : "—"),
                    null,
                    "Télécharger la mise en demeure",
                    documentUrl,
                    true
            ));
        }

        if (tenant != null) {
            notificationService.create(tenant.getId(), NotificationType.MISE_EN_DEMEURE,
                    "Mise en demeure",
                    String.format("Une mise en demeure vous a été adressée pour le paiement de %.2f €.", payment.getAmount()),
                    "/payments");
        }
        notificationService.create(owner.getId(), NotificationType.MISE_EN_DEMEURE,
                "Mise en demeure envoyée",
                String.format("La mise en demeure a été envoyée à %s pour %.2f € (%s). Le document est archivé.",
                        tenant != null ? tenant.getFullName() : "—", payment.getAmount(),
                        rental.getProperty().getName()),
                "/payments");

        payment.setRelanceNiveau(NIVEAU_MISE_EN_DEMEURE);
        payment.setDerniereRelanceLe(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Mise en demeure envoyée pour le paiement {}", payment.getId());
    }

    private void checkOwnerAccess(Payment payment) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        if (!payment.getRental().getProperty().getOwner().getId().equals(currentUserId)
                && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à relancer ce paiement.");
        }
    }

    private String adresse(Rental rental) {
        var property = rental.getProperty();
        return String.format("%s, %s %s",
                property.getAddressLine1() != null ? property.getAddressLine1() : "",
                property.getPostalCode() != null ? property.getPostalCode() : "",
                property.getCity() != null ? property.getCity() : "");
    }

    private String buildMiseEnDemeureHtml(Payment payment) {
        Rental rental = payment.getRental();
        User owner = rental.getProperty().getOwner();
        User tenant = payment.getPayer();

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 16pt; margin-bottom: 5px;">MISE EN DEMEURE DE PAYER</h1>
                    <p style="font-size: 10pt; color: #666;">Lettre recommandée avec accusé de réception (ou remise contre signature)</p>
                </div>

                <table style="width: 100%%; border: none; margin-bottom: 20px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Bailleur :</strong><br/>%s</td>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Locataire :</strong><br/>%s</td>
                    </tr>
                </table>

                <p><strong>Adresse du bien loué :</strong> %s</p>
                <p><strong>Date :</strong> %s</p>

                <p style="margin-top: 15px;">Madame, Monsieur,</p>

                <p>Malgré notre relance, nous constatons que la somme suivante demeure impayée à ce jour :</p>

                <table style="margin-top: 15px; width: 90%%; margin-left: auto; margin-right: auto;">
                    <tr><td style="padding: 8px;"><strong>Nature</strong></td><td style="padding: 8px; text-align: right;">%s</td></tr>
                    <tr><td style="padding: 8px;"><strong>Montant dû</strong></td><td style="padding: 8px; text-align: right;"><strong>%.2f €</strong></td></tr>
                    <tr><td style="padding: 8px;"><strong>Échéance</strong></td><td style="padding: 8px; text-align: right;">%s</td></tr>
                </table>

                <p style="margin-top: 20px;">En conséquence, nous vous mettons en demeure de procéder au règlement
                intégral de cette somme dans un délai de <strong>huit (8) jours</strong> à compter de la réception
                de la présente.</p>

                <p>À défaut de paiement dans ce délai, nous nous réservons le droit d'engager toute procédure utile,
                notamment la mise en jeu de la clause résolutoire prévue au bail et les actions prévues par
                l'article 24 de la loi n° 89-462 du 6 juillet 1989, sans autre avis ni délai.</p>

                <p style="margin-top: 25px;">Veuillez agréer, Madame, Monsieur, l'expression de nos salutations distinguées.</p>

                <p style="margin-top: 30px; text-align: right;">%s</p>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Document généré par Howners à la demande du bailleur. La présente mise en demeure ne fait pas
                    obstacle à un règlement amiable. Si votre situation le justifie, vous pouvez saisir le fonds de
                    solidarité pour le logement (FSL) ou contacter l'ADIL de votre département.
                </p>
                """.formatted(
                owner.getFullName(),
                tenant != null ? tenant.getFullName() : "N/A",
                adresse(rental),
                LocalDate.now().format(FR_DATE),
                payment.getPaymentType() != null ? switch (payment.getPaymentType()) {
                    case RENT -> "Loyer";
                    case CHARGES -> "Charges";
                    case DEPOSIT -> "Dépôt de garantie";
                    default -> "Autre";
                } : "Loyer",
                payment.getAmount(),
                payment.getDueDate() != null ? payment.getDueDate().format(FR_DATE) : "—",
                owner.getFullName());
    }
}
