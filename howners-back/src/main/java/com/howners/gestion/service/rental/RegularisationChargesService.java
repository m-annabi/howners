package com.howners.gestion.service.rental;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.rental.ChargeRegularisation;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.StatutRegularisation;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.rental.RegularisationResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ChargeRegularisationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Régularisation annuelle des charges locatives.
 *
 * Règle de calcul V1 (exclusive, pour éviter tout double comptage) :
 * - Provisions = somme des Payments de type CHARGES au statut PAID sur l'année civile.
 *   Si aucun paiement CHARGES distinct n'existe, fallback : rental.charges × nombre de
 *   mois d'occupation sur l'année (provisions incluses dans le loyer).
 * - Charges réelles = dépenses du bien sur la période d'occupation, limitées aux
 *   catégories récupérables (UTILITIES, CONDO_FEES, CLEANING, MAINTENANCE).
 *   Le prorata de récupérabilité fine (ex. 50 % d'une facture) n'est pas géré en V1.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegularisationChargesService {

    static final Set<ExpenseCategory> CATEGORIES_RECUPERABLES = Set.of(
            ExpenseCategory.UTILITIES,
            ExpenseCategory.CONDO_FEES,
            ExpenseCategory.CLEANING,
            ExpenseCategory.MAINTENANCE
    );

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChargeRegularisationRepository regularisationRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<RegularisationResponse> findByRentalId(UUID rentalId) {
        getRentalAndCheckReadAccess(rentalId);
        return regularisationRepository.findByRentalIdOrderByAnneeDesc(rentalId).stream()
                .map(RegularisationResponse::from)
                .toList();
    }

    @Transactional
    public RegularisationResponse calculer(UUID rentalId, int annee) {
        Rental rental = getRentalAndCheckOwnerAccess(rentalId);

        if (annee >= LocalDate.now().getYear() && LocalDate.now().getMonthValue() < 12) {
            log.info("Régularisation calculée en cours d'année {} pour la location {}", annee, rentalId);
        }
        regularisationRepository.findByRentalIdAndAnnee(rentalId, annee).ifPresent(r -> {
            throw new BusinessException("Une régularisation existe déjà pour l'année " + annee
                    + ". Supprimez-la ou consultez-la avant d'en recréer une.");
        });

        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        LocalDate finAnnee = LocalDate.of(annee, 12, 31);
        LocalDate debutOccupation = max(debutAnnee, rental.getStartDate());
        LocalDate finOccupation = rental.getEndDate() != null ? min(finAnnee, rental.getEndDate()) : finAnnee;

        if (debutOccupation.isAfter(finOccupation)) {
            throw new BusinessException("La location n'était pas occupée sur l'année " + annee + ".");
        }

        // Provisions : paiements CHARGES payés sur l'année, sinon forfait mensuel du bail
        BigDecimal provisions = paymentRepository.findByRentalId(rentalId).stream()
                .filter(p -> p.getPaymentType() == PaymentType.CHARGES)
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getDueDate() != null && p.getDueDate().getYear() == annee)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (provisions.compareTo(BigDecimal.ZERO) == 0 && rental.getCharges() != null) {
            long moisOccupes = ChronoUnit.MONTHS.between(
                    debutOccupation.withDayOfMonth(1), finOccupation.withDayOfMonth(1)) + 1;
            provisions = rental.getCharges().multiply(BigDecimal.valueOf(moisOccupes));
        }

        // Charges réelles récupérables sur la période d'occupation
        final LocalDate debut = debutOccupation;
        final LocalDate fin = finOccupation;
        List<Expense> depenses = expenseRepository.findByPropertyId(rental.getProperty().getId()).stream()
                .filter(e -> CATEGORIES_RECUPERABLES.contains(e.getCategory()))
                .filter(e -> e.getExpenseDate() != null
                        && !e.getExpenseDate().isBefore(debut)
                        && !e.getExpenseDate().isAfter(fin))
                .toList();

        Map<String, Object> detail = new LinkedHashMap<>();
        BigDecimal chargesReelles = BigDecimal.ZERO;
        for (Expense e : depenses) {
            String cat = e.getCategory().name();
            BigDecimal cumul = (BigDecimal) detail.getOrDefault(cat, BigDecimal.ZERO);
            detail.put(cat, cumul.add(e.getAmount()));
            chargesReelles = chargesReelles.add(e.getAmount());
        }

        BigDecimal solde = chargesReelles.subtract(provisions).setScale(2, RoundingMode.HALF_UP);

        ChargeRegularisation regul = ChargeRegularisation.builder()
                .rental(rental)
                .annee(annee)
                .provisionsEncaissees(provisions.setScale(2, RoundingMode.HALF_UP))
                .chargesReelles(chargesReelles.setScale(2, RoundingMode.HALF_UP))
                .solde(solde)
                .detail(detail)
                .statut(StatutRegularisation.BROUILLON)
                .build();

        regul = regularisationRepository.save(regul);
        log.info("Régularisation {} calculée pour la location {} : provisions={}, réelles={}, solde={}",
                annee, rentalId, provisions, chargesReelles, solde);
        return RegularisationResponse.from(regul);
    }

    @Transactional
    public RegularisationResponse envoyerDecompte(UUID regularisationId) {
        ChargeRegularisation regul = getRegulAndCheckOwnerAccess(regularisationId);

        if (regul.getStatut() != StatutRegularisation.BROUILLON) {
            throw new BadRequestException("Le décompte a déjà été envoyé.");
        }

        Rental rental = regul.getRental();
        User tenant = rental.getTenant();

        String html = buildDecompteHtml(regul);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdf(html, null);
        } catch (IOException e) {
            throw new RuntimeException("Échec de génération du décompte de charges", e);
        }

        String fileName = String.format("decompte_charges_%d_%s.pdf", regul.getAnnee(), regul.getId());
        String fileKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

        Document document = Document.builder()
                .rental(rental)
                .property(rental.getProperty())
                .uploader(rental.getProperty().getOwner())
                .documentType(DocumentType.OTHER)
                .fileName(fileName)
                .filePath(fileKey)
                .fileKey(fileKey)
                .fileSize((long) pdfBytes.length)
                .mimeType("application/pdf")
                .documentHash(pdfService.calculateHash(pdfBytes))
                .description("Décompte de régularisation des charges " + regul.getAnnee())
                .build();
        document = documentRepository.save(document);
        regul.setDocument(document);
        regul.setStatut(StatutRegularisation.ENVOYEE);
        regul = regularisationRepository.save(regul);

        if (tenant != null) {
            String sens = regul.getSolde().compareTo(BigDecimal.ZERO) >= 0
                    ? "complément à régler : " + regul.getSolde().abs() + " €"
                    : "trop-perçu à vous restituer : " + regul.getSolde().abs() + " €";
            notificationService.create(
                    tenant.getId(),
                    NotificationType.CHARGE_REGULARIZATION,
                    "Régularisation des charges " + regul.getAnnee(),
                    "Le décompte annuel des charges est disponible — " + sens + ".",
                    "/rentals/" + rental.getId());

            if (tenant.getEmail() != null) {
                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        "Régularisation des charges " + regul.getAnnee() + " — " + rental.getProperty().getName(),
                        "Régularisation des charges",
                        "Le décompte annuel de régularisation des charges locatives de votre logement est disponible.",
                        String.format(
                                "Provisions versées : <strong>%.2f €</strong><br/>"
                                        + "Charges réelles : <strong>%.2f €</strong><br/>"
                                        + "Solde : <strong>%.2f €</strong> (%s)",
                                regul.getProvisionsEncaissees(), regul.getChargesReelles(),
                                regul.getSolde(),
                                regul.getSolde().compareTo(BigDecimal.ZERO) >= 0
                                        ? "complément dû" : "à vous restituer"),
                        "Voir le détail",
                        null,
                        false
                ));
            }
        }

        log.info("Décompte de régularisation {} envoyé", regularisationId);
        return RegularisationResponse.from(regul);
    }

    /**
     * Crée un paiement complémentaire CHARGES (PENDING, échéance +30 jours) si le solde est dû.
     */
    @Transactional
    public RegularisationResponse creerPaiementComplementaire(UUID regularisationId) {
        ChargeRegularisation regul = getRegulAndCheckOwnerAccess(regularisationId);

        if (regul.getStatut() != StatutRegularisation.ENVOYEE) {
            throw new BadRequestException("Le décompte doit d'abord être envoyé au locataire.");
        }
        if (regul.getSolde().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Aucun complément dû : le solde est nul ou en faveur du locataire.");
        }
        Rental rental = regul.getRental();
        if (rental.getTenant() == null) {
            throw new BadRequestException("Aucun locataire associé à cette location.");
        }

        Payment payment = Payment.builder()
                .rental(rental)
                .payer(rental.getTenant())
                .paymentType(PaymentType.CHARGES)
                .amount(regul.getSolde())
                .currency(rental.getCurrency() != null ? rental.getCurrency() : "EUR")
                .status(PaymentStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        paymentRepository.save(payment);

        regul.setStatut(StatutRegularisation.SOLDEE);
        regul = regularisationRepository.save(regul);

        log.info("Paiement complémentaire de {} € créé pour la régularisation {}", regul.getSolde(), regularisationId);
        return RegularisationResponse.from(regul);
    }

    @Transactional(readOnly = true)
    public byte[] downloadDecompte(UUID regularisationId) throws IOException {
        ChargeRegularisation regul = regularisationRepository.findById(regularisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularisation", "id", regularisationId.toString()));
        checkReadAccess(regul.getRental());
        if (regul.getDocument() == null || regul.getDocument().getFileKey() == null) {
            throw new BadRequestException("Aucun décompte disponible pour cette régularisation.");
        }
        return storageService.downloadFile(regul.getDocument().getFileKey());
    }

    // ----- Helpers -----

    private static LocalDate max(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }

    private Rental getRentalAndCheckOwnerAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à gérer les régularisations de cette location.");
        }
        return rental;
    }

    private ChargeRegularisation getRegulAndCheckOwnerAccess(UUID regularisationId) {
        ChargeRegularisation regul = regularisationRepository.findById(regularisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularisation", "id", regularisationId.toString()));
        getRentalAndCheckOwnerAccess(regul.getRental().getId());
        return regul;
    }

    private void getRentalAndCheckReadAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        checkReadAccess(rental);
    }

    private void checkReadAccess(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);
        if (!isOwner && !isTenant && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à consulter ces régularisations.");
        }
    }

    private String buildDecompteHtml(ChargeRegularisation regul) {
        Rental rental = regul.getRental();
        var property = rental.getProperty();
        User owner = property.getOwner();
        User tenant = rental.getTenant();

        String adresse = String.format("%s, %s %s",
                property.getAddressLine1() != null ? property.getAddressLine1() : "",
                property.getPostalCode() != null ? property.getPostalCode() : "",
                property.getCity() != null ? property.getCity() : "");

        StringBuilder lignes = new StringBuilder();
        if (regul.getDetail() != null) {
            regul.getDetail().forEach((cat, montant) -> lignes.append(String.format(
                    "<tr><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px; text-align: right;\">%s €</td></tr>",
                    libelleCategorie(cat), montant)));
        }

        boolean complementDu = regul.getSolde().compareTo(BigDecimal.ZERO) >= 0;

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 16pt; margin-bottom: 5px;">DÉCOMPTE DE RÉGULARISATION DES CHARGES</h1>
                    <p style="font-size: 10pt; color: #666;">Année %d</p>
                </div>

                <table style="width: 100%%; border: none; margin-bottom: 20px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Bailleur :</strong><br/>%s</td>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Locataire :</strong><br/>%s</td>
                    </tr>
                </table>

                <p><strong>Adresse du bien :</strong> %s</p>
                <p><strong>Date :</strong> %s</p>

                <h3 style="margin-top: 20px;">Charges récupérables de l'année %d</h3>
                <table style="width: 90%%; margin-left: auto; margin-right: auto;">
                    %s
                    <tr style="border-top: 1px solid #999;"><td style="padding: 6px;"><strong>Total charges réelles</strong></td><td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td></tr>
                </table>

                <table style="margin-top: 20px; width: 90%%; margin-left: auto; margin-right: auto;">
                    <tr><td style="padding: 8px;"><strong>Provisions encaissées</strong></td><td style="padding: 8px; text-align: right;">%.2f €</td></tr>
                    <tr style="border-top: 2px solid #333;"><td style="padding: 8px;"><strong>%s</strong></td><td style="padding: 8px; text-align: right;"><strong>%.2f €</strong></td></tr>
                </table>

                <p style="margin-top: 25px;">%s</p>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Les justificatifs des charges sont tenus à votre disposition pendant six mois à compter
                    de l'envoi de ce décompte (article 23 de la loi n° 89-462 du 6 juillet 1989).
                </p>
                """.formatted(
                regul.getAnnee(),
                owner.getFullName(),
                tenant != null ? tenant.getFullName() : "N/A",
                adresse,
                LocalDate.now().format(FR_DATE),
                regul.getAnnee(),
                lignes.toString(),
                regul.getChargesReelles(),
                regul.getProvisionsEncaissees(),
                complementDu ? "Complément dû par le locataire" : "Trop-perçu à restituer au locataire",
                regul.getSolde().abs(),
                complementDu
                        ? "Le montant du complément est à régler dans un délai de 30 jours."
                        : "Le trop-perçu vous sera restitué ou déduit de votre prochaine échéance.");
    }

    private String libelleCategorie(String categorie) {
        return switch (categorie) {
            case "UTILITIES" -> "Eau, énergie, services";
            case "CONDO_FEES" -> "Charges de copropriété";
            case "CLEANING" -> "Entretien et nettoyage";
            case "MAINTENANCE" -> "Maintenance courante";
            default -> categorie;
        };
    }
}
