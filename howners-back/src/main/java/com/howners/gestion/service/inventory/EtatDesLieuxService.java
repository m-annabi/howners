package com.howners.gestion.service.inventory;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.inventory.EtatDesLieux;
import com.howners.gestion.domain.inventory.EtatDesLieuxType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.inventory.CreateEtatDesLieuxRequest;
import com.howners.gestion.dto.inventory.EtatDesLieuxResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.EtatDesLieuxRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.util.PdfFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtatDesLieuxService {

    private final EtatDesLieuxRepository edlRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EtatDesLieuxResponse> findByRentalId(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
        checkRentalAccess(rental);
        return edlRepository.findByRentalIdOrderByInspectionDateDesc(rentalId)
                .stream()
                .map(EtatDesLieuxResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EtatDesLieuxResponse> findByCurrentUser() {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            return edlRepository.findAll().stream().map(EtatDesLieuxResponse::from).toList();
        }

        return edlRepository.findByOwnerId(userId)
                .stream()
                .map(EtatDesLieuxResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EtatDesLieuxResponse findById(UUID id) {
        EtatDesLieux edl = edlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etat des lieux not found"));
        checkRentalAccess(edl.getRental());
        return EtatDesLieuxResponse.from(edl);
    }

    @Transactional
    public EtatDesLieuxResponse create(UUID rentalId, CreateEtatDesLieuxRequest request) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
        checkRentalAccess(rental);

        EtatDesLieux edl = EtatDesLieux.builder()
                .rental(rental)
                .type(request.type())
                .inspectionDate(request.inspectionDate())
                .roomConditions(request.roomConditions())
                .meterReadings(request.meterReadings())
                .keysCount(request.keysCount())
                .keysDescription(request.keysDescription())
                .generalComments(request.generalComments())
                .createdBy(currentUser)
                .build();

        // Generate PDF
        String html = buildEdlHtml(edl, rental);
        String typeLabel = request.type() == EtatDesLieuxType.ENTREE ? "entree" : "sortie";
        byte[] pdfBytes = pdfService.generatePdf(html, "État des lieux - " + typeLabel);

        String fileKey = storageService.uploadFile(pdfBytes,
                "edl/edl-" + typeLabel + "-" + rental.getId() + ".pdf",
                "application/pdf");

        Document doc = Document.builder()
                .rental(rental)
                .fileName("edl-" + typeLabel + "-" + rental.getProperty().getName() + ".pdf")
                .fileKey(fileKey)
                .mimeType("application/pdf")
                .fileSize((long) pdfBytes.length)
                .documentType(DocumentType.OTHER)
                .uploader(currentUser)
                .build();
        doc = documentRepository.save(doc);
        edl.setDocument(doc);

        edl = edlRepository.save(edl);
        auditService.logAction(AuditAction.CREATE, "EtatDesLieux", edl.getId());
        log.info("Etat des lieux {} created for rental {}", typeLabel, rentalId);

        return EtatDesLieuxResponse.from(edl);
    }

    @Transactional
    public EtatDesLieuxResponse sign(UUID edlId, String signerRole) {
        UUID currentUserId = AuthService.getCurrentUserId();
        EtatDesLieux edl = edlRepository.findById(edlId)
                .orElseThrow(() -> new ResourceNotFoundException("Etat des lieux not found"));
        checkRentalAccess(edl.getRental());

        if ("OWNER".equalsIgnoreCase(signerRole)) {
            if (Boolean.TRUE.equals(edl.getOwnerSigned())) {
                throw new BadRequestException("Owner already signed");
            }
            edl.setOwnerSigned(true);
            edl.setOwnerSignedAt(LocalDateTime.now());
        } else if ("TENANT".equalsIgnoreCase(signerRole)) {
            if (Boolean.TRUE.equals(edl.getTenantSigned())) {
                throw new BadRequestException("Tenant already signed");
            }
            edl.setTenantSigned(true);
            edl.setTenantSignedAt(LocalDateTime.now());
        } else {
            throw new BadRequestException("Invalid signer role: " + signerRole);
        }

        edl = edlRepository.save(edl);
        auditService.logAction(AuditAction.UPDATE, "EtatDesLieux", edl.getId(),
                "{\"action\": \"sign\", \"role\": \"" + signerRole + "\"}");
        return EtatDesLieuxResponse.from(edl);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(UUID edlId) throws IOException {
        EtatDesLieux edl = edlRepository.findById(edlId)
                .orElseThrow(() -> new ResourceNotFoundException("Etat des lieux not found"));
        checkRentalAccess(edl.getRental());

        if (edl.getDocument() == null) {
            throw new BadRequestException("No PDF document for this état des lieux");
        }

        return storageService.downloadFile(edl.getDocument().getFileKey());
    }

    private void checkRentalAccess(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = rental.getProperty().getOwner().getId();
        UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != Role.ADMIN && !ownerId.equals(currentUserId) && !currentUserId.equals(tenantId)) {
            throw new ForbiddenException("You are not authorized to access this état des lieux");
        }
    }

    private String buildEdlHtml(EtatDesLieux edl, Rental rental) {
        String ownerName = rental.getProperty().getOwner().getFullName();
        String tenantName = rental.getTenant() != null ? rental.getTenant().getFullName() : "Locataire non renseigné";
        String propertyName = rental.getProperty().getName();
        String propertyAddress = PdfFormat.adressePostale(rental.getProperty());
        String typeLabel = edl.getType() == EtatDesLieuxType.ENTREE ? "ENTRÉE" : "SORTIE";

        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; padding: 40px;'>");
        html.append("<h1 style='text-align: center;'>ÉTAT DES LIEUX DE ").append(typeLabel).append("</h1>");
        html.append("<hr/>");

        html.append("<h3>Parties</h3>");
        html.append("<p>Bailleur : <strong>").append(ownerName).append("</strong></p>");
        html.append("<p>Locataire : <strong>").append(tenantName).append("</strong></p>");

        html.append("<h3>Bien concerné</h3>");
        html.append("<p>").append(propertyName).append(" - ").append(propertyAddress).append("</p>");

        html.append("<h3>Date de l'état des lieux</h3>");
        html.append("<p>").append(edl.getInspectionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>");

        List<Map<String, String>> rooms = parseRooms(edl.getRoomConditions());
        if (!rooms.isEmpty()) {
            html.append("<h3>État des pièces</h3>");
            html.append("<table><tr><th>Pièce</th><th>État</th><th>Observations</th></tr>");
            for (Map<String, String> r : rooms) {
                html.append("<tr><td>").append(nl2br(r.get("name")))
                    .append("</td><td>").append(nl2br(r.getOrDefault("condition", "")))
                    .append("</td><td>").append(nl2br(r.getOrDefault("comments", ""))).append("</td></tr>");
            }
            html.append("</table>");
        }

        Map<String, String> meters = parseMeters(edl.getMeterReadings());
        if (!meters.isEmpty()) {
            html.append("<h3>Relevés des compteurs</h3>");
            html.append("<table><tr><th>Compteur</th><th>Relevé</th></tr>");
            for (Map.Entry<String, String> m : meters.entrySet()) {
                html.append("<tr><td>").append(nl2br(m.getKey()))
                    .append("</td><td>").append(nl2br(m.getValue())).append("</td></tr>");
            }
            html.append("</table>");
        }

        if (edl.getKeysCount() != null) {
            html.append("<h3>Clés</h3>");
            html.append("<p>Nombre de clés : <strong>").append(edl.getKeysCount()).append("</strong></p>");
            if (edl.getKeysDescription() != null) {
                html.append("<p>Description : ").append(nl2br(edl.getKeysDescription())).append("</p>");
            }
        }

        if (edl.getGeneralComments() != null) {
            html.append("<h3>Commentaires généraux</h3>");
            html.append("<p>").append(nl2br(edl.getGeneralComments())).append("</p>");
        }

        html.append("<div style='margin-top: 60px;'>");
        html.append("<table style='width: 100%;'><tr>");
        html.append("<td style='width: 50%;'><p>Le Bailleur</p><br/><br/><p>").append(ownerName).append("</p></td>");
        html.append("<td style='width: 50%;'><p>Le Locataire</p><br/><br/><p>").append(tenantName).append("</p></td>");
        html.append("</tr></table>");
        html.append("</div>");

        html.append("<p style='font-size: 10px; margin-top: 30px; color: #666;'>");
        html.append("Document généré le ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        html.append("</p>");
        html.append("</div>");

        return html.toString();
    }

    /** Échappe le HTML d'un champ libre et convertit les retours à la ligne en &lt;br/&gt;. */
    private String nl2br(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br/>");
    }

    private static final ObjectMapper EDL_MAPPER = new ObjectMapper();

    /** Parse le JSON des pièces [{name, condition, comments}] pour le rendu PDF. */
    private List<Map<String, String>> parseRooms(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> raw = EDL_MAPPER.readValue(json, new TypeReference<>() {});
            List<Map<String, String>> result = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                Map<String, String> piece = new LinkedHashMap<>();
                piece.put("name", str(item.get("name")));
                piece.put("condition", str(item.get("condition")));
                piece.put("comments", str(item.get("comments")));
                if (piece.get("name") != null && !piece.get("name").isBlank()) result.add(piece);
            }
            return result;
        } catch (Exception e) {
            log.warn("Pièces EDL illisibles pour le PDF : {}", e.getMessage());
            return List.of();
        }
    }

    /** Parse le JSON des compteurs [{type, value}] pour le rendu PDF. */
    private Map<String, String> parseMeters(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;
        try {
            List<Map<String, Object>> raw = EDL_MAPPER.readValue(json, new TypeReference<>() {});
            for (Map<String, Object> item : raw) {
                String type = str(item.get("type"));
                if (type != null && !type.isBlank()) result.put(type, str(item.get("value")));
            }
        } catch (Exception e) {
            log.warn("Compteurs EDL illisibles pour le PDF : {}", e.getMessage());
        }
        return result;
    }

    private static String str(Object v) {
        return v != null ? String.valueOf(v) : "";
    }
}
