package com.howners.gestion.service.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.inventory.EdlComparaison;
import com.howners.gestion.domain.inventory.EtatDesLieux;
import com.howners.gestion.domain.inventory.EtatDesLieuxType;
import com.howners.gestion.domain.inventory.StatutComparaison;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.inventory.ComparaisonEdlResponse;
import com.howners.gestion.dto.inventory.RetenueDepotRequest;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.EdlComparaisonRepository;
import com.howners.gestion.repository.EtatDesLieuxRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comparatif EDL entrée/sortie : diff des pièces (matching par nom normalisé), des relevés
 * de compteurs et des clés, puis saisie de retenues sur le dépôt de garantie (montants
 * libres, plafonnés au dépôt — pas de chiffrage automatique ni de vétusté en V1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EdlComparisonService {

    /** Ordre de dégradation : NEUF(0) → MAUVAIS(4). */
    private static final List<String> ORDRE_ETATS = List.of("NEUF", "BON", "CORRECT", "USAGE", "MAUVAIS");
    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EdlComparaisonRepository comparaisonRepository;
    private final EtatDesLieuxRepository etatDesLieuxRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ComparaisonEdlResponse comparer(UUID rentalId) {
        Rental rental = getRentalAndCheckReadAccess(rentalId);
        EdlComparaison existante = comparaisonRepository.findByRentalId(rentalId).orElse(null);

        // Le locataire ne voit le comparatif qu'une fois validé
        if (isCurrentUserTenantOnly(rental)
                && (existante == null || existante.getStatut() != StatutComparaison.VALIDEE)) {
            throw new ForbiddenException("Le comparatif n'a pas encore été validé par le bailleur.");
        }

        return buildResponse(rental, existante);
    }

    @Transactional
    public ComparaisonEdlResponse enregistrerRetenues(UUID rentalId, RetenueDepotRequest request) {
        Rental rental = getRentalAndCheckOwnerAccess(rentalId);

        EdlComparaison comparaison = comparaisonRepository.findByRentalId(rentalId)
                .orElseGet(() -> initComparaison(rental));

        if (comparaison.getStatut() == StatutComparaison.VALIDEE) {
            throw new BadRequestException("Le comparatif a déjà été validé.");
        }

        BigDecimal total = request.retenues().stream()
                .map(RetenueDepotRequest.Retenue::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal depot = rental.getDepositAmount() != null ? rental.getDepositAmount() : BigDecimal.ZERO;
        if (total.compareTo(depot) > 0) {
            throw new BusinessException(String.format(
                    "Le total des retenues (%.2f €) dépasse le dépôt de garantie (%.2f €).", total, depot));
        }

        try {
            comparaison.setRetenues(objectMapper.writeValueAsString(request.retenues()));
        } catch (IOException e) {
            throw new BadRequestException("Format de retenues invalide.");
        }
        comparaison.setTotalRetenues(total);
        comparaison.setSoldeARestituer(depot.subtract(total));
        comparaison = comparaisonRepository.save(comparaison);

        return buildResponse(rental, comparaison);
    }

    @Transactional
    public ComparaisonEdlResponse valider(UUID rentalId) {
        Rental rental = getRentalAndCheckOwnerAccess(rentalId);
        EdlComparaison comparaison = comparaisonRepository.findByRentalId(rentalId)
                .orElseGet(() -> comparaisonRepository.save(initComparaison(rental)));

        if (comparaison.getStatut() == StatutComparaison.VALIDEE) {
            throw new BadRequestException("Le comparatif a déjà été validé.");
        }

        ComparaisonEdlResponse diff = buildResponse(rental, comparaison);

        // PDF
        String html = buildPdfHtml(rental, diff);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdf(html, null);
        } catch (IOException e) {
            throw new RuntimeException("Échec de génération du comparatif EDL", e);
        }

        String fileName = String.format("comparatif_edl_%s_%d.pdf", rentalId, System.currentTimeMillis());
        String fileKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

        Document document = Document.builder()
                .rental(rental)
                .property(rental.getProperty())
                .uploader(rental.getProperty().getOwner())
                .documentType(DocumentType.INVENTORY)
                .fileName(fileName)
                .filePath(fileKey)
                .fileKey(fileKey)
                .fileSize((long) pdfBytes.length)
                .mimeType("application/pdf")
                .documentHash(pdfService.calculateHash(pdfBytes))
                .description("Comparatif état des lieux entrée/sortie + retenues sur dépôt")
                .build();
        document = documentRepository.save(document);

        comparaison.setDocument(document);
        comparaison.setStatut(StatutComparaison.VALIDEE);
        comparaison = comparaisonRepository.save(comparaison);

        User tenant = rental.getTenant();
        if (tenant != null) {
            String sens = comparaison.getSoldeARestituer() != null
                    ? String.format("Solde de dépôt à restituer : %.2f €.", comparaison.getSoldeARestituer())
                    : "";
            notificationService.create(
                    tenant.getId(),
                    NotificationType.SYSTEM,
                    "Comparatif d'état des lieux disponible",
                    "Le comparatif entrée/sortie et le décompte du dépôt de garantie sont disponibles. " + sens,
                    "/inventory");

            if (tenant.getEmail() != null) {
                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        "Comparatif d'état des lieux — " + rental.getProperty().getName(),
                        "État des lieux de sortie",
                        "Le comparatif entre l'état des lieux d'entrée et de sortie de votre logement a été établi.",
                        String.format("Total des retenues : <strong>%.2f €</strong><br/>Solde à restituer : <strong>%.2f €</strong>",
                                comparaison.getTotalRetenues(),
                                comparaison.getSoldeARestituer() != null ? comparaison.getSoldeARestituer() : BigDecimal.ZERO),
                        null,
                        null,
                        false
                ));
            }
        }

        log.info("Comparatif EDL validé pour la location {}", rentalId);
        return buildResponse(rental, comparaison);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(UUID comparaisonId) throws IOException {
        EdlComparaison comparaison = comparaisonRepository.findById(comparaisonId)
                .orElseThrow(() -> new ResourceNotFoundException("Comparaison", "id", comparaisonId.toString()));
        getRentalAndCheckReadAccess(comparaison.getRental().getId());
        if (comparaison.getDocument() == null || comparaison.getDocument().getFileKey() == null) {
            throw new BadRequestException("Aucun PDF disponible pour ce comparatif.");
        }
        return storageService.downloadFile(comparaison.getDocument().getFileKey());
    }

    // ----- Construction du diff -----

    private EdlComparaison initComparaison(Rental rental) {
        EtatDesLieux entree = etatDesLieuxRepository
                .findByRentalIdAndType(rental.getId(), EtatDesLieuxType.ENTREE)
                .orElseThrow(() -> new BusinessException("Aucun état des lieux d'entrée pour cette location."));
        EtatDesLieux sortie = etatDesLieuxRepository
                .findByRentalIdAndType(rental.getId(), EtatDesLieuxType.SORTIE)
                .orElseThrow(() -> new BusinessException("Aucun état des lieux de sortie pour cette location."));
        return EdlComparaison.builder()
                .rental(rental)
                .edlEntree(entree)
                .edlSortie(sortie)
                .build();
    }

    private ComparaisonEdlResponse buildResponse(Rental rental, EdlComparaison comparaison) {
        EtatDesLieux entree;
        EtatDesLieux sortie;
        if (comparaison != null && comparaison.getEdlEntree() != null && comparaison.getEdlSortie() != null) {
            entree = comparaison.getEdlEntree();
            sortie = comparaison.getEdlSortie();
        } else {
            entree = etatDesLieuxRepository
                    .findByRentalIdAndType(rental.getId(), EtatDesLieuxType.ENTREE)
                    .orElseThrow(() -> new BusinessException("Aucun état des lieux d'entrée pour cette location."));
            sortie = etatDesLieuxRepository
                    .findByRentalIdAndType(rental.getId(), EtatDesLieuxType.SORTIE)
                    .orElseThrow(() -> new BusinessException("Aucun état des lieux de sortie pour cette location."));
        }

        List<Map<String, String>> piecesEntree = parseRooms(entree.getRoomConditions());
        List<Map<String, String>> piecesSortie = parseRooms(sortie.getRoomConditions());

        Map<String, Map<String, String>> sortieParNom = new LinkedHashMap<>();
        for (Map<String, String> piece : piecesSortie) {
            sortieParNom.put(normaliser(piece.get("name")), piece);
        }

        List<ComparaisonEdlResponse.PieceComparee> pieces = new ArrayList<>();
        for (Map<String, String> pieceEntree : piecesEntree) {
            String nom = pieceEntree.get("name");
            Map<String, String> pieceSortie = sortieParNom.remove(normaliser(nom));
            if (pieceSortie == null) {
                pieces.add(new ComparaisonEdlResponse.PieceComparee(
                        nom, pieceEntree.get("condition"), null,
                        pieceEntree.get("comments"), null, false, true));
                continue;
            }
            String etatEntree = pieceEntree.get("condition");
            String etatSortie = pieceSortie.get("condition");
            int idxEntree = ORDRE_ETATS.indexOf(etatEntree != null ? etatEntree.toUpperCase() : "");
            int idxSortie = ORDRE_ETATS.indexOf(etatSortie != null ? etatSortie.toUpperCase() : "");
            boolean comparable = idxEntree >= 0 && idxSortie >= 0;
            boolean degradee = comparable && idxSortie > idxEntree;
            pieces.add(new ComparaisonEdlResponse.PieceComparee(
                    nom, etatEntree, etatSortie,
                    pieceEntree.get("comments"), pieceSortie.get("comments"),
                    degradee, !comparable));
        }
        // Pièces présentes uniquement à la sortie
        for (Map<String, String> pieceSortie : sortieParNom.values()) {
            pieces.add(new ComparaisonEdlResponse.PieceComparee(
                    pieceSortie.get("name"), null, pieceSortie.get("condition"),
                    null, pieceSortie.get("comments"), false, true));
        }

        // Compteurs
        Map<String, String> compteursEntree = parseMeters(entree.getMeterReadings());
        Map<String, String> compteursSortie = parseMeters(sortie.getMeterReadings());
        List<ComparaisonEdlResponse.CompteurCompare> compteurs = new ArrayList<>();
        for (var entry : compteursEntree.entrySet()) {
            compteurs.add(new ComparaisonEdlResponse.CompteurCompare(
                    entry.getKey(), entry.getValue(), compteursSortie.remove(entry.getKey())));
        }
        compteursSortie.forEach((type, valeur) ->
                compteurs.add(new ComparaisonEdlResponse.CompteurCompare(type, null, valeur)));

        // Retenues persistées
        List<ComparaisonEdlResponse.RetenueDepot> retenues = List.of();
        if (comparaison != null && comparaison.getRetenues() != null) {
            try {
                retenues = objectMapper.readValue(comparaison.getRetenues(),
                        new TypeReference<List<ComparaisonEdlResponse.RetenueDepot>>() {});
            } catch (IOException e) {
                log.warn("Retenues illisibles pour la comparaison {} : {}",
                        comparaison.getId(), e.getMessage());
            }
        }

        BigDecimal depot = rental.getDepositAmount();
        BigDecimal totalRetenues = comparaison != null ? comparaison.getTotalRetenues() : BigDecimal.ZERO;
        BigDecimal solde = comparaison != null && comparaison.getSoldeARestituer() != null
                ? comparaison.getSoldeARestituer()
                : (depot != null ? depot.subtract(totalRetenues) : null);

        return new ComparaisonEdlResponse(
                comparaison != null ? comparaison.getId() : null,
                rental.getId(),
                entree.getId(),
                sortie.getId(),
                entree.getInspectionDate(),
                sortie.getInspectionDate(),
                pieces,
                compteurs,
                entree.getKeysCount(),
                sortie.getKeysCount(),
                retenues,
                totalRetenues,
                depot,
                solde,
                comparaison != null ? comparaison.getStatut() : StatutComparaison.BROUILLON,
                comparaison != null && comparaison.getDocument() != null
                        ? comparaison.getDocument().getId() : null
        );
    }

    /** Parsing défensif : les JSONB legacy peuvent être hétérogènes. */
    private List<Map<String, String>> parseRooms(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<Map<String, String>> result = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                Map<String, String> piece = new LinkedHashMap<>();
                piece.put("name", stringValue(item.get("name")));
                piece.put("condition", stringValue(item.get("condition")));
                piece.put("comments", stringValue(item.get("comments")));
                if (piece.get("name") != null && !piece.get("name").isBlank()) {
                    result.add(piece);
                }
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(
                    "Le format des pièces de l'état des lieux est illisible — comparaison impossible.");
        }
    }

    private Map<String, String> parseMeters(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            for (Map<String, Object> item : raw) {
                String type = stringValue(item.get("type"));
                if (type != null && !type.isBlank()) {
                    result.put(type, stringValue(item.get("value")));
                }
            }
        } catch (IOException e) {
            log.warn("Relevés de compteurs illisibles : {}", e.getMessage());
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static String normaliser(String nom) {
        return nom != null ? nom.trim().toLowerCase() : "";
    }

    // ----- Accès -----

    private Rental getRentalAndCheckOwnerAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à gérer le comparatif de cette location.");
        }
        return rental;
    }

    private Rental getRentalAndCheckReadAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);
        if (!isOwner && !isTenant && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à consulter ce comparatif.");
        }
        return rental;
    }

    private boolean isCurrentUserTenantOnly(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);
        return isTenant && !isOwner;
    }

    // ----- PDF -----

    private String buildPdfHtml(Rental rental, ComparaisonEdlResponse diff) {
        var property = rental.getProperty();
        User owner = property.getOwner();
        User tenant = rental.getTenant();

        StringBuilder lignesPieces = new StringBuilder();
        for (var piece : diff.pieces()) {
            String style = piece.degradee() ? " style=\"background-color: #fdf2f2;\"" : "";
            lignesPieces.append(String.format(
                    "<tr%s><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px; font-size: 8pt;\">%s</td></tr>",
                    style,
                    piece.nom(),
                    piece.etatEntree() != null ? piece.etatEntree() : "—",
                    piece.etatSortie() != null ? piece.etatSortie() : "—",
                    piece.degradee() ? "Dégradation constatée" : (piece.nonComparable() ? "Non comparable" : "")));
        }

        StringBuilder lignesRetenues = new StringBuilder();
        for (var retenue : diff.retenues()) {
            lignesRetenues.append(String.format(
                    "<tr><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px;\">%s</td><td style=\"padding: 6px; text-align: right;\">%.2f €</td></tr>",
                    retenue.piece(), retenue.motif(), retenue.montant()));
        }
        if (diff.retenues().isEmpty()) {
            lignesRetenues.append("<tr><td colspan=\"3\" style=\"padding: 6px; font-style: italic;\">Aucune retenue</td></tr>");
        }

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 16pt; margin-bottom: 5px;">COMPARATIF D'ÉTAT DES LIEUX</h1>
                    <p style="font-size: 10pt; color: #666;">Entrée du %s — Sortie du %s</p>
                </div>

                <table style="width: 100%%; border: none; margin-bottom: 20px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Bailleur :</strong><br/>%s</td>
                        <td style="border: none; width: 50%%; vertical-align: top;"><strong>Locataire :</strong><br/>%s</td>
                    </tr>
                </table>

                <p><strong>Bien :</strong> %s</p>

                <h3 style="margin-top: 20px;">État des pièces</h3>
                <table style="width: 100%%;">
                    <tr>
                        <th style="padding: 6px; text-align: left;">Pièce</th>
                        <th style="padding: 6px; text-align: left;">Entrée</th>
                        <th style="padding: 6px; text-align: left;">Sortie</th>
                        <th style="padding: 6px; text-align: left;"></th>
                    </tr>
                    %s
                </table>

                <h3 style="margin-top: 20px;">Retenues sur le dépôt de garantie</h3>
                <table style="width: 100%%;">
                    <tr>
                        <th style="padding: 6px; text-align: left;">Pièce</th>
                        <th style="padding: 6px; text-align: left;">Motif</th>
                        <th style="padding: 6px; text-align: right;">Montant</th>
                    </tr>
                    %s
                    <tr style="border-top: 2px solid #333;">
                        <td colspan="2" style="padding: 6px;"><strong>Total des retenues</strong></td>
                        <td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td>
                    </tr>
                    <tr>
                        <td colspan="2" style="padding: 6px;"><strong>Dépôt de garantie</strong></td>
                        <td style="padding: 6px; text-align: right;">%.2f €</td>
                    </tr>
                    <tr>
                        <td colspan="2" style="padding: 6px;"><strong>Solde à restituer au locataire</strong></td>
                        <td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td>
                    </tr>
                </table>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Le dépôt de garantie doit être restitué dans un délai d'un mois (deux mois en cas de retenues)
                    à compter de la remise des clés (article 22 de la loi n° 89-462 du 6 juillet 1989).
                    Les retenues doivent être justifiées (devis, factures).
                </p>
                """.formatted(
                diff.dateEntree() != null ? diff.dateEntree().format(FR_DATE) : "—",
                diff.dateSortie() != null ? diff.dateSortie().format(FR_DATE) : "—",
                owner.getFullName(),
                tenant != null ? tenant.getFullName() : "N/A",
                property.getName(),
                lignesPieces,
                lignesRetenues,
                diff.totalRetenues(),
                diff.depositAmount() != null ? diff.depositAmount() : BigDecimal.ZERO,
                diff.soldeARestituer() != null ? diff.soldeARestituer() : BigDecimal.ZERO);
    }
}
