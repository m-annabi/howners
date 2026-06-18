package com.howners.gestion.service.rgpd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.audit.ConsentType;
import com.howners.gestion.domain.audit.UserConsent;
import com.howners.gestion.domain.rgpd.RgpdRequest;
import com.howners.gestion.domain.rgpd.RgpdRequestStatus;
import com.howners.gestion.domain.rgpd.RgpdRequestType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.audit.ConsentRequest;
import com.howners.gestion.dto.audit.ConsentResponse;
import com.howners.gestion.dto.audit.UserDataExportResponse;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.*;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RgpdService {

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final RgpdRequestRepository rgpdRequestRepository;
    private final AuditService auditService;
    private final PdfService pdfService;
    private final StorageService storageService;

    @Transactional
    public UserDataExportResponse exportUserData() {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditService.logAction(AuditAction.DATA_EXPORT, "User", userId);
        recordCompletedRequest(user, RgpdRequestType.EXPORT, "Export des données personnelles fourni.");

        var personalInfo = new UserDataExportResponse.UserInfo(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
        );

        var properties = propertyRepository.findByOwnerId(userId).stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId().toString());
                    map.put("name", p.getName());
                    map.put("address", p.getAddressLine1());
                    map.put("city", p.getCity());
                    map.put("createdAt", p.getCreatedAt().toString());
                    return map;
                }).collect(Collectors.toList());

        var rentals = rentalRepository.findByOwnerId(userId).stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", r.getId().toString());
                    map.put("status", r.getStatus().name());
                    map.put("startDate", r.getStartDate() != null ? r.getStartDate().toString() : null);
                    map.put("monthlyRent", r.getMonthlyRent());
                    return map;
                }).collect(Collectors.toList());

        var contracts = contractRepository.findByOwnerId(userId).stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", c.getId().toString());
                    map.put("contractNumber", c.getContractNumber());
                    map.put("status", c.getStatus().name());
                    map.put("createdAt", c.getCreatedAt().toString());
                    return map;
                }).collect(Collectors.toList());

        var payments = paymentRepository.findByPayerId(userId).stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId().toString());
                    map.put("amount", p.getAmount());
                    map.put("status", p.getStatus().name());
                    map.put("dueDate", p.getDueDate() != null ? p.getDueDate().toString() : null);
                    return map;
                }).collect(Collectors.toList());

        var documents = documentRepository.findByUploaderId(userId).stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", d.getId().toString());
                    map.put("fileName", d.getFileName());
                    map.put("documentType", d.getDocumentType() != null ? d.getDocumentType().name() : null);
                    map.put("createdAt", d.getCreatedAt().toString());
                    return map;
                }).collect(Collectors.toList());

        var consents = userConsentRepository.findByUserId(userId).stream()
                .map(ConsentResponse::from)
                .toList();

        return new UserDataExportResponse(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                personalInfo,
                properties,
                rentals,
                contracts,
                payments,
                documents,
                consents
        );
    }

    @Transactional
    public byte[] exportUserDataAsPdf() throws IOException {
        UserDataExportResponse data = exportUserData();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        StringBuilder html = new StringBuilder();
        html.append("<h1>Export de données personnelles - RGPD</h1>");
        html.append("<p><strong>Date d'export :</strong> ").append(data.exportDate()).append("</p>");

        html.append("<h2>Informations personnelles</h2>");
        html.append("<table><tr><td>Email</td><td>").append(data.personalInfo().email()).append("</td></tr>");
        html.append("<tr><td>Prénom</td><td>").append(data.personalInfo().firstName()).append("</td></tr>");
        html.append("<tr><td>Nom</td><td>").append(data.personalInfo().lastName()).append("</td></tr>");
        html.append("<tr><td>Téléphone</td><td>").append(data.personalInfo().phone()).append("</td></tr>");
        html.append("<tr><td>Rôle</td><td>").append(data.personalInfo().role()).append("</td></tr>");
        html.append("<tr><td>Inscrit le</td><td>").append(data.personalInfo().createdAt()).append("</td></tr></table>");

        html.append("<h2>Propriétés (").append(data.properties().size()).append(")</h2>");
        html.append("<h2>Locations (").append(data.rentals().size()).append(")</h2>");
        html.append("<h2>Contrats (").append(data.contracts().size()).append(")</h2>");
        html.append("<h2>Paiements (").append(data.payments().size()).append(")</h2>");
        html.append("<h2>Documents (").append(data.documents().size()).append(")</h2>");

        html.append("<h2>Consentements</h2>");
        for (ConsentResponse consent : data.consents()) {
            html.append("<p>").append(consent.consentType()).append(" : ")
                    .append(consent.granted() ? "Accordé" : "Refusé").append("</p>");
        }

        return pdfService.generatePdf(html.toString(), "Export RGPD");
    }

    private static final String PII_DELETED = "[supprimé-rgpd]";
    private static final String MESSAGE_SCRUBBED = "[Supprimé à la demande de l'utilisateur]";

    @Transactional
    public void anonymizeUser() {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsAnonymized())) {
            throw new BusinessException("User already anonymized");
        }

        // 1. Trace la demande d'effacement (délai légal d'1 mois) dès sa réception.
        RgpdRequest request = rgpdRequestRepository.save(RgpdRequest.builder()
                .user(user)
                .type(RgpdRequestType.ERASURE)
                .status(RgpdRequestStatus.RECEIVED)
                .build());

        // 2. Pseudonymise les données personnelles du compte.
        String anonymizedHash = UUID.randomUUID().toString().substring(0, 8);
        user.setEmail("anonymized-" + anonymizedHash + "@deleted.howners.com");
        user.setFirstName("Anonyme");
        user.setLastName("Utilisateur");
        user.setPhone(null);
        user.setPasswordHash("ANONYMIZED");
        user.setEnabled(false);
        user.setIsAnonymized(true);
        user.setAnonymizedAt(LocalDateTime.now());
        userRepository.save(user);

        // 3. Documents : conscient de la conservation légale.
        //    - Pièces personnelles (PII) → suppression du fichier S3 + effacement des
        //      métadonnées de la ligne (le nom de fichier peut contenir du PII).
        //    - Documents à obligation légale → conservés sous legal hold (exception art. 17-3).
        int deleted = 0;
        int retained = 0;
        for (var doc : documentRepository.findByUploaderId(userId)) {
            if (DocumentRetentionPolicy.isRetainedOnErasure(doc.getDocumentType())) {
                doc.setLegalHold(true);
                documentRepository.save(doc);
                retained++;
                continue;
            }
            if (doc.getFileKey() != null && !PII_DELETED.equals(doc.getFileKey())) {
                try {
                    storageService.deleteFile(doc.getFileKey());
                } catch (Exception e) {
                    log.warn("Échec suppression S3 du fichier {} (effacement RGPD) : {}",
                            doc.getFileKey(), e.getMessage());
                }
            }
            // Champs NOT NULL → placeholder ; le reste effacé. La ligne reste comme tombstone auditable.
            doc.setFileName(PII_DELETED);
            doc.setFilePath(PII_DELETED);
            doc.setFileKey(PII_DELETED);
            doc.setFileUrl(null);
            doc.setDescription(null);
            doc.setMetadata(null);
            doc.setDocumentHash(null);
            doc.setIsArchived(true);
            documentRepository.save(doc);
            deleted++;
        }

        // 4. Messages émis par l'utilisateur : contenu anonymisé (les messages reçus,
        //    rédigés par des tiers, ne sont pas touchés).
        int scrubbedMessages = 0;
        for (var message : messageRepository.findBySenderId(userId)) {
            message.setSubject(null);
            message.setBody(MESSAGE_SCRUBBED);
            messageRepository.save(message);
            scrubbedMessages++;
        }

        // 5. Clôture la demande + audit.
        request.markCompleted(String.format(
                "%d document(s) PII supprimé(s), %d conservé(s) (obligation légale), %d message(s) anonymisé(s)",
                deleted, retained, scrubbedMessages));
        rgpdRequestRepository.save(request);

        auditService.logAction(AuditAction.DATA_ERASURE, "User", userId);
        log.info("Utilisateur {} effacé (RGPD) — {} docs supprimés, {} conservés, {} messages anonymisés",
                userId, deleted, retained, scrubbedMessages);
    }

    private void recordCompletedRequest(User user, RgpdRequestType type, String details) {
        RgpdRequest request = RgpdRequest.builder()
                .user(user)
                .type(type)
                .status(RgpdRequestStatus.RECEIVED)
                .build();
        request.markCompleted(details);
        rgpdRequestRepository.save(request);
    }

    @Transactional
    public ConsentResponse recordConsent(ConsentRequest request) {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserConsent consent = userConsentRepository
                .findByUserIdAndConsentType(userId, request.consentType())
                .orElseGet(() -> UserConsent.builder()
                        .user(user)
                        .consentType(request.consentType())
                        .build());

        consent.setGranted(request.granted());
        if (request.granted()) {
            consent.setGrantedAt(LocalDateTime.now());
            consent.setRevokedAt(null);
        } else {
            consent.setRevokedAt(LocalDateTime.now());
        }
        consent.setIpAddress(getClientIpAddress());

        consent = userConsentRepository.save(consent);
        return ConsentResponse.from(consent);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getUserConsents() {
        UUID userId = AuthService.getCurrentUserId();
        return userConsentRepository.findByUserId(userId).stream()
                .map(ConsentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteConsent(UUID consentId) {
        UUID userId = AuthService.getCurrentUserId();
        UserConsent consent = userConsentRepository.findById(consentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent not found"));
        if (!consent.getUser().getId().equals(userId)) {
            throw new BusinessException("Cannot delete another user's consent");
        }
        userConsentRepository.delete(consent);
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
            }
        } catch (Exception e) {
            // No request context
        }
        return null;
    }
}
