package com.howners.gestion.service.document;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * Upload un document
     */
    @Transactional
    public DocumentResponse uploadDocument(
            MultipartFile file,
            DocumentType documentType,
            UUID propertyId,
            UUID rentalId,
            UUID applicationId,
            String description) throws IOException {

        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Valider le fichier
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Valider la taille (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 10MB limit");
        }

        // Récupérer les entités associées si fournies
        Property property = null;
        if (propertyId != null) {
            property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new RuntimeException("Property not found"));

            // Vérifier que l'utilisateur est le propriétaire
            if (!property.getOwner().getId().equals(currentUserId)) {
                throw new RuntimeException("You are not authorized to upload documents for this property");
            }
        }

        Rental rental = null;
        if (rentalId != null) {
            rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new RuntimeException("Rental not found"));

            // Vérifier que l'utilisateur est le propriétaire ou le locataire
            UUID ownerId = rental.getProperty().getOwner().getId();
            UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;

            if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId)) {
                throw new RuntimeException("You are not authorized to upload documents for this rental");
            }
        }

        Application application = null;
        if (applicationId != null) {
            application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Vérifier que l'utilisateur est le candidat de cette candidature
            if (!application.getApplicant().getId().equals(currentUserId)) {
                throw new RuntimeException("You are not authorized to upload documents for this application");
            }
        }

        // Calculer le hash du fichier
        byte[] fileBytes = file.getBytes();
        String documentHash = calculateHash(fileBytes);

        // Upload vers MinIO
        String fileName = file.getOriginalFilename();
        String fileKey = storageService.uploadFile(fileBytes, fileName, file.getContentType());

        // Créer le document (sans fileUrl, généré à la volée)
        Document document = Document.builder()
                .fileName(fileName)
                .filePath(fileKey)  // Pour compatibilité
                .fileKey(fileKey)   // Nouveau champ pour accès direct
                .fileUrl(null)      // Ne pas stocker l'URL
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .documentType(documentType)
                .property(property)
                .rental(rental)
                .application(application)
                .uploader(currentUser)
                .documentHash(documentHash)
                .description(description)  // Persister la description
                .uploadedAt(LocalDateTime.now())
                .build();

        document = documentRepository.save(document);

        log.info("Document uploaded: {} by user {}", fileName, currentUser.getEmail());

        return toResponse(document);
    }

    /**
     * Récupérer tous les documents de l'utilisateur
     */
    public List<DocumentResponse> getMyDocuments() {
        UUID currentUserId = AuthService.getCurrentUserId();

        return documentRepository.findByUploaderId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les documents d'une propriété
     */
    public List<DocumentResponse> getPropertyDocuments(UUID propertyId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Vérifier les permissions
        if (!property.getOwner().getId().equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to view documents for this property");
        }

        return documentRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les documents d'une location
     */
    public List<DocumentResponse> getRentalDocuments(UUID rentalId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("Rental not found"));

        // Vérifier les permissions (propriétaire ou locataire)
        UUID ownerId = rental.getProperty().getOwner().getId();
        UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId)) {
            throw new RuntimeException("You are not authorized to view documents for this rental");
        }

        return documentRepository.findByRentalId(rentalId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les documents d'une candidature
     */
    public List<DocumentResponse> getApplicationDocuments(UUID applicationId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Le candidat, le propriétaire de l'annonce, ou un admin peut voir les documents
        boolean isApplicant = application.getApplicant().getId().equals(currentUserId);
        boolean isOwner = application.getListing().getProperty().getOwner().getId().equals(currentUserId);

        if (!isApplicant && !isOwner) {
            throw new RuntimeException("You are not authorized to view documents for this application");
        }

        return documentRepository.findByApplicationId(applicationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer un document par ID
     */
    public DocumentResponse getDocument(UUID documentId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Vérifier les permissions
        boolean isOwner = false;

        if (document.getProperty() != null) {
            isOwner = document.getProperty().getOwner().getId().equals(currentUserId);
        } else if (document.getRental() != null) {
            UUID ownerId = document.getRental().getProperty().getOwner().getId();
            UUID tenantId = document.getRental().getTenant() != null ?
                    document.getRental().getTenant().getId() : null;
            isOwner = ownerId.equals(currentUserId) || currentUserId.equals(tenantId);
        } else if (document.getApplication() != null) {
            boolean isApplicant = document.getApplication().getApplicant().getId().equals(currentUserId);
            boolean isListingOwner = document.getApplication().getListing().getProperty().getOwner().getId().equals(currentUserId);
            isOwner = isApplicant || isListingOwner;
        }

        boolean isUploader = document.getUploader().getId().equals(currentUserId);

        if (!isOwner && !isUploader) {
            throw new RuntimeException("You are not authorized to view this document");
        }

        return toResponse(document);
    }

    /**
     * Télécharger un document
     */
    public byte[] downloadDocument(UUID documentId) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Vérifier les permissions (même logique que getDocument)
        boolean isOwner = false;

        if (document.getProperty() != null) {
            isOwner = document.getProperty().getOwner().getId().equals(currentUserId);
        } else if (document.getRental() != null) {
            UUID ownerId = document.getRental().getProperty().getOwner().getId();
            UUID tenantId = document.getRental().getTenant() != null ?
                    document.getRental().getTenant().getId() : null;
            isOwner = ownerId.equals(currentUserId) || currentUserId.equals(tenantId);
        } else if (document.getApplication() != null) {
            boolean isApplicant = document.getApplication().getApplicant().getId().equals(currentUserId);
            boolean isListingOwner = document.getApplication().getListing().getProperty().getOwner().getId().equals(currentUserId);
            isOwner = isApplicant || isListingOwner;
        }

        boolean isUploader = document.getUploader().getId().equals(currentUserId);

        if (!isOwner && !isUploader) {
            throw new RuntimeException("You are not authorized to download this document");
        }

        // Utiliser directement la clé stockée
        String fileKey = document.getFileKey();
        return storageService.downloadFile(fileKey);
    }

    /**
     * Supprimer un document
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Seul l'uploader ou le propriétaire peut supprimer
        boolean isOwner = false;

        if (document.getProperty() != null) {
            isOwner = document.getProperty().getOwner().getId().equals(currentUserId);
        } else if (document.getRental() != null) {
            isOwner = document.getRental().getProperty().getOwner().getId().equals(currentUserId);
        } else if (document.getApplication() != null) {
            isOwner = document.getApplication().getApplicant().getId().equals(currentUserId);
        }

        boolean isUploader = document.getUploader().getId().equals(currentUserId);

        if (!isOwner && !isUploader) {
            throw new RuntimeException("You are not authorized to delete this document");
        }

        // Supprimer de MinIO
        String fileKey = document.getFileKey();
        storageService.deleteFile(fileKey);

        // Supprimer de la base de données
        documentRepository.delete(document);

        log.info("Document deleted: {} by user {}", document.getFileName(), currentUserId);
    }

    /**
     * Calculer le hash SHA-256 d'un fichier
     */
    private String calculateHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating hash: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate document hash", e);
        }
    }

    /**
     * Convertit un Document en DocumentResponse avec URL présignée générée à la volée
     */
    public DocumentResponse toResponse(Document document) {
        String fileUrl = storageService.generatePresignedUrl(document.getFileKey());

        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                fileUrl, // URL présignée générée à la volée
                document.getFileSize(),
                document.getMimeType(),
                document.getDocumentType(),
                document.getProperty() != null ? document.getProperty().getId() : null,
                document.getRental() != null ? document.getRental().getId() : null,
                document.getApplication() != null ? document.getApplication().getId() : null,
                document.getUploader().getId(),
                getFullName(document.getUploader()),
                document.getDocumentHash(),
                document.getDescription(),
                document.getUploadedAt(),
                document.getRetentionEndDate(),
                document.getArchivedAt(),
                document.getIsArchived(),
                document.getLegalHold()
        );
    }

    private String getFullName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

}
