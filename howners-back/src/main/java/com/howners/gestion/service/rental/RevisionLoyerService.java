package com.howners.gestion.service.rental;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.rental.IrlIndice;
import com.howners.gestion.domain.rental.RentRevision;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.rental.StatutRevision;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.rental.CreateIrlIndiceRequest;
import com.howners.gestion.dto.rental.IrlIndiceResponse;
import com.howners.gestion.dto.rental.RevisionLoyerResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.IrlIndiceRepository;
import com.howners.gestion.repository.RentRevisionRepository;
import com.howners.gestion.repository.RentalRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Révision annuelle de loyer indexée sur l'IRL (loi 89-462, art. 17-1).
 * Le nouveau loyer est plafonné à : ancien loyer × (IRL nouveau / IRL ancien),
 * indices du même trimestre que la date de début du bail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevisionLoyerService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RentRevisionRepository revisionRepository;
    private final IrlIndiceRepository irlIndiceRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ----- Indices IRL -----

    @Transactional(readOnly = true)
    public List<IrlIndiceResponse> getIndices() {
        return irlIndiceRepository.findAllByOrderByAnneeDescTrimestreDesc().stream()
                .map(IrlIndiceResponse::from)
                .toList();
    }

    @Transactional
    public IrlIndiceResponse addIndice(CreateIrlIndiceRequest request) {
        irlIndiceRepository.findByAnneeAndTrimestre(request.annee(), request.trimestre())
                .ifPresent(i -> {
                    throw new BadRequestException("L'indice IRL " + request.annee() + "-T" + request.trimestre() + " existe déjà.");
                });
        IrlIndice indice = IrlIndice.builder()
                .annee(request.annee())
                .trimestre(request.trimestre())
                .valeur(request.valeur())
                .build();
        return IrlIndiceResponse.from(irlIndiceRepository.save(indice));
    }

    // ----- Révisions -----

    @Transactional(readOnly = true)
    public List<RevisionLoyerResponse> findByRentalId(UUID rentalId) {
        Rental rental = getRentalAndCheckReadAccess(rentalId);
        boolean isTenant = rental.getTenant() != null
                && rental.getTenant().getId().equals(AuthService.getCurrentUserId());
        return revisionRepository.findByRentalIdOrderByCreatedAtDesc(rentalId).stream()
                // Le locataire ne voit que les révisions qui lui ont été notifiées
                .filter(r -> !isTenant || r.getStatut() == StatutRevision.NOTIFIEE
                        || r.getStatut() == StatutRevision.APPLIQUEE)
                .map(RevisionLoyerResponse::from)
                .toList();
    }

    @Transactional
    public RevisionLoyerResponse calculerRevision(UUID rentalId) {
        Rental rental = getRentalAndCheckOwnerAccess(rentalId);

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BadRequestException("La révision de loyer n'est possible que sur une location active.");
        }
        if (revisionRepository.existsByRentalIdAndDateRevisionAfterAndStatutNot(
                rentalId, LocalDate.now().minusMonths(11), StatutRevision.ANNULEE)) {
            throw new BusinessException("Une révision a déjà été effectuée il y a moins d'un an pour cette location.");
        }

        int trimestre = ((rental.getStartDate().getMonthValue() - 1) / 3) + 1;

        IrlIndice indiceNouveau = irlIndiceRepository.findTopByTrimestreOrderByAnneeDesc(trimestre)
                .orElseThrow(() -> new BusinessException(
                        "Aucun indice IRL disponible pour le trimestre T" + trimestre
                                + ". Ajoutez les indices INSEE récents avant de calculer la révision."));

        IrlIndice indiceAncien = irlIndiceRepository
                .findByAnneeAndTrimestre(indiceNouveau.getAnnee() - 1, trimestre)
                .orElseThrow(() -> new BusinessException(
                        "L'indice IRL " + (indiceNouveau.getAnnee() - 1) + "-T" + trimestre
                                + " est manquant — impossible de calculer la variation annuelle."));

        BigDecimal ancienLoyer = rental.getMonthlyRent();
        BigDecimal nouveauLoyer = ancienLoyer
                .multiply(indiceNouveau.getValeur())
                .divide(indiceAncien.getValeur(), 2, RoundingMode.HALF_UP);

        RentRevision revision = RentRevision.builder()
                .rental(rental)
                .ancienLoyer(ancienLoyer)
                .nouveauLoyer(nouveauLoyer)
                .indiceAncien(indiceAncien)
                .indiceNouveau(indiceNouveau)
                .dateRevision(LocalDate.now())
                .dateEffet(prochainAnniversaire(rental.getStartDate()))
                .statut(StatutRevision.BROUILLON)
                .build();

        revision = revisionRepository.save(revision);
        log.info("Révision calculée pour la location {} : {} -> {} €", rentalId, ancienLoyer, nouveauLoyer);
        return RevisionLoyerResponse.from(revision);
    }

    @Transactional
    public RevisionLoyerResponse notifierRevision(UUID revisionId) {
        RentRevision revision = getRevisionAndCheckOwnerAccess(revisionId);

        if (revision.getStatut() != StatutRevision.BROUILLON) {
            throw new BadRequestException("Seule une révision en brouillon peut être notifiée.");
        }

        Rental rental = revision.getRental();
        User tenant = rental.getTenant();

        // Courrier PDF
        String html = buildCourrierHtml(revision);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdf(html, null);
        } catch (IOException e) {
            throw new RuntimeException("Échec de génération du courrier de révision", e);
        }

        String fileName = String.format("revision_loyer_%s_%d.pdf", revision.getId(), System.currentTimeMillis());
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
                .description("Courrier de révision de loyer IRL")
                .build();
        document = documentRepository.save(document);
        revision.setDocument(document);
        revision.setStatut(StatutRevision.NOTIFIEE);
        revision = revisionRepository.save(revision);

        // Notifications + email locataire
        if (tenant != null) {
            notificationService.create(
                    tenant.getId(),
                    NotificationType.RENT_REVISION,
                    "Révision de votre loyer",
                    String.format("Votre loyer passe de %.2f € à %.2f € au %s (indexation IRL).",
                            revision.getAncienLoyer(), revision.getNouveauLoyer(),
                            revision.getDateEffet().format(FR_DATE)),
                    "/rentals/" + rental.getId());

            if (tenant.getEmail() != null) {
                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        "Révision annuelle de votre loyer — " + rental.getProperty().getName(),
                        "Révision de loyer",
                        String.format(
                                "Conformément à la clause de révision de votre bail et à l'article 17-1 de la loi n° 89-462, "
                                        + "votre loyer est révisé selon l'indice de référence des loyers (IRL) publié par l'INSEE."),
                        String.format(
                                "Ancien loyer : <strong>%.2f €</strong><br/>"
                                        + "Nouveau loyer : <strong>%.2f €</strong><br/>"
                                        + "Indice de référence : IRL %d-T%d (%.2f) / IRL %d-T%d (%.2f)<br/>"
                                        + "Date d'effet : <strong>%s</strong>",
                                revision.getAncienLoyer(), revision.getNouveauLoyer(),
                                revision.getIndiceNouveau().getAnnee(), revision.getIndiceNouveau().getTrimestre(),
                                revision.getIndiceNouveau().getValeur(),
                                revision.getIndiceAncien().getAnnee(), revision.getIndiceAncien().getTrimestre(),
                                revision.getIndiceAncien().getValeur(),
                                revision.getDateEffet().format(FR_DATE)),
                        "Voir ma location",
                        null,
                        false
                ));
            }
        }

        notificationService.create(
                rental.getProperty().getOwner().getId(),
                NotificationType.RENT_REVISION,
                "Révision de loyer notifiée",
                "Le courrier de révision a été envoyé au locataire pour " + rental.getProperty().getName() + ".",
                "/rentals/" + rental.getId());

        log.info("Révision {} notifiée au locataire", revisionId);
        return RevisionLoyerResponse.from(revision);
    }

    @Transactional
    public RevisionLoyerResponse appliquerRevision(UUID revisionId) {
        RentRevision revision = getRevisionAndCheckOwnerAccess(revisionId);

        if (revision.getStatut() != StatutRevision.NOTIFIEE) {
            throw new BadRequestException("La révision doit être notifiée au locataire avant d'être appliquée.");
        }

        Rental rental = revision.getRental();
        rental.setMonthlyRent(revision.getNouveauLoyer());
        rentalRepository.save(rental);

        revision.setStatut(StatutRevision.APPLIQUEE);
        revision = revisionRepository.save(revision);

        log.info("Révision {} appliquée : loyer de la location {} mis à jour à {} €",
                revisionId, rental.getId(), revision.getNouveauLoyer());
        return RevisionLoyerResponse.from(revision);
    }

    @Transactional
    public RevisionLoyerResponse annulerRevision(UUID revisionId) {
        RentRevision revision = getRevisionAndCheckOwnerAccess(revisionId);
        if (revision.getStatut() == StatutRevision.APPLIQUEE) {
            throw new BadRequestException("Une révision déjà appliquée ne peut pas être annulée.");
        }
        revision.setStatut(StatutRevision.ANNULEE);
        return RevisionLoyerResponse.from(revisionRepository.save(revision));
    }

    @Transactional(readOnly = true)
    public byte[] downloadCourrier(UUID revisionId) throws IOException {
        RentRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision", "id", revisionId.toString()));
        checkReadAccess(revision.getRental());
        if (revision.getDocument() == null || revision.getDocument().getFileKey() == null) {
            throw new BadRequestException("Aucun courrier disponible pour cette révision.");
        }
        return storageService.downloadFile(revision.getDocument().getFileKey());
    }

    /**
     * Détection quotidienne des révisions dues : anniversaire du bail dans ~30 jours
     * et aucune révision sur les 11 derniers mois.
     */
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void detecterRevisionsDues() {
        LocalDate cible = LocalDate.now().plusDays(30);
        List<Rental> actives = rentalRepository.findByStatus(RentalStatus.ACTIVE);

        for (Rental rental : actives) {
            LocalDate start = rental.getStartDate();
            if (start == null || rental.getMonthlyRent() == null) continue;
            // Anniversaire du bail dans 30 jours (même jour/mois), bail d'au moins 1 an
            if (start.getDayOfMonth() != cible.getDayOfMonth()
                    || start.getMonthValue() != cible.getMonthValue()
                    || start.getYear() >= cible.getYear()) {
                continue;
            }
            if (revisionRepository.existsByRentalIdAndDateRevisionAfterAndStatutNot(
                    rental.getId(), LocalDate.now().minusMonths(11), StatutRevision.ANNULEE)) {
                continue;
            }
            notificationService.create(
                    rental.getProperty().getOwner().getId(),
                    NotificationType.RENT_REVISION,
                    "Révision de loyer possible",
                    String.format("L'anniversaire du bail de %s approche (%s) — vous pouvez réviser le loyer selon l'IRL.",
                            rental.getProperty().getName(), cible.format(FR_DATE)),
                    "/rentals/" + rental.getId());
            log.info("Révision due détectée pour la location {}", rental.getId());
        }
    }

    // ----- Helpers -----

    private LocalDate prochainAnniversaire(LocalDate startDate) {
        LocalDate anniversaire = startDate.withYear(LocalDate.now().getYear());
        if (!anniversaire.isAfter(LocalDate.now())) {
            anniversaire = anniversaire.plusYears(1);
        }
        return anniversaire;
    }

    private Rental getRentalAndCheckOwnerAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à gérer les révisions de cette location.");
        }
        return rental;
    }

    private RentRevision getRevisionAndCheckOwnerAccess(UUID revisionId) {
        RentRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision", "id", revisionId.toString()));
        getRentalAndCheckOwnerAccess(revision.getRental().getId());
        return revision;
    }

    private Rental getRentalAndCheckReadAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        checkReadAccess(rental);
        return rental;
    }

    private void checkReadAccess(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);
        if (!isOwner && !isTenant && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à consulter les révisions de cette location.");
        }
    }

    private String buildCourrierHtml(RentRevision revision) {
        Rental rental = revision.getRental();
        var property = rental.getProperty();
        User owner = property.getOwner();
        User tenant = rental.getTenant();

        String adresse = String.format("%s, %s %s",
                property.getAddressLine1() != null ? property.getAddressLine1() : "",
                property.getPostalCode() != null ? property.getPostalCode() : "",
                property.getCity() != null ? property.getCity() : "");

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 16pt; margin-bottom: 5px;">RÉVISION ANNUELLE DE LOYER</h1>
                    <p style="font-size: 10pt; color: #666;">Indexation sur l'indice de référence des loyers (IRL)</p>
                </div>

                <table style="width: 100%%; border: none; margin-bottom: 20px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Bailleur :</strong><br/>%s
                        </td>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Locataire :</strong><br/>%s
                        </td>
                    </tr>
                </table>

                <p><strong>Adresse du bien :</strong> %s</p>
                <p><strong>Date :</strong> %s</p>

                <p style="margin-top: 15px;">Madame, Monsieur,</p>

                <p>Conformément à la clause de révision prévue dans votre contrat de location et à
                l'article 17-1 de la loi n° 89-462 du 6 juillet 1989, je vous informe de la révision
                annuelle de votre loyer, calculée sur la base de l'indice de référence des loyers (IRL)
                publié par l'INSEE.</p>

                <table style="margin-top: 20px; width: 90%%; margin-left: auto; margin-right: auto;">
                    <tr><td style="padding: 8px;"><strong>Indice de référence (ancien)</strong></td><td style="padding: 8px; text-align: right;">IRL %d-T%d : %.2f</td></tr>
                    <tr><td style="padding: 8px;"><strong>Indice de référence (nouveau)</strong></td><td style="padding: 8px; text-align: right;">IRL %d-T%d : %.2f</td></tr>
                    <tr><td style="padding: 8px;"><strong>Loyer actuel (hors charges)</strong></td><td style="padding: 8px; text-align: right;">%.2f €</td></tr>
                    <tr style="border-top: 2px solid #333;"><td style="padding: 8px;"><strong>Nouveau loyer (hors charges)</strong></td><td style="padding: 8px; text-align: right;"><strong>%.2f €</strong></td></tr>
                    <tr><td style="padding: 8px;"><strong>Date d'effet</strong></td><td style="padding: 8px; text-align: right;">%s</td></tr>
                </table>

                <p style="margin-top: 20px;">Formule appliquée : nouveau loyer = loyer actuel × (IRL nouveau / IRL ancien).</p>

                <p style="margin-top: 25px;">Je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées.</p>

                <p style="margin-top: 30px; text-align: right;">%s</p>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Document généré par Howners. La révision ne peut excéder la variation de l'IRL sur un an
                    (article 17-1, loi n° 89-462 du 6 juillet 1989).
                </p>
                """.formatted(
                owner.getFullName(),
                tenant != null ? tenant.getFullName() : "N/A",
                adresse,
                LocalDate.now().format(FR_DATE),
                revision.getIndiceAncien().getAnnee(), revision.getIndiceAncien().getTrimestre(),
                revision.getIndiceAncien().getValeur(),
                revision.getIndiceNouveau().getAnnee(), revision.getIndiceNouveau().getTrimestre(),
                revision.getIndiceNouveau().getValeur(),
                revision.getAncienLoyer(),
                revision.getNouveauLoyer(),
                revision.getDateEffet().format(FR_DATE),
                owner.getFullName());
    }
}
