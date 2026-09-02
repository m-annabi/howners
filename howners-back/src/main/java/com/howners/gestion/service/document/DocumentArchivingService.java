package com.howners.gestion.service.document;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentArchivingService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * Autorise la gestion du cycle de vie d'un document (rétention/archivage/legal hold) au
     * propriétaire du bien concerné, à l'uploader, ou à un admin. Le contrôle @PreAuthorize du
     * contrôleur ne garantit que le rôle, pas la propriété de l'objet : sans ce contrôle, un
     * bailleur pourrait archiver/altérer les documents d'un autre (IDOR).
     */
    private void requireDocumentAccess(Document document) {
        UUID uid = AuthService.getCurrentUserId();
        UUID ownerId = null;
        if (document.getProperty() != null && document.getProperty().getOwner() != null) {
            ownerId = document.getProperty().getOwner().getId();
        } else if (document.getRental() != null && document.getRental().getProperty() != null) {
            ownerId = document.getRental().getProperty().getOwner().getId();
        } else if (document.getApplication() != null && document.getApplication().getListing() != null) {
            ownerId = document.getApplication().getListing().getProperty().getOwner().getId();
        }
        boolean isOwner = ownerId != null && ownerId.equals(uid);
        boolean isUploader = document.getUploader() != null && document.getUploader().getId().equals(uid);
        boolean isAdmin = userRepository.findById(uid).map(u -> u.getRole() == Role.ADMIN).orElse(false);
        if (!isOwner && !isUploader && !isAdmin) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à gérer ce document.");
        }
    }

    @Transactional
    public DocumentResponse setRetentionPeriod(UUID documentId, LocalDate retentionEndDate) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        requireDocumentAccess(document);

        if (document.getIsArchived()) {
            throw new BadRequestException("Cannot set retention on archived document");
        }

        document.setRetentionEndDate(retentionEndDate);
        document = documentRepository.save(document);
        log.info("Retention period set for document {} until {}", documentId, retentionEndDate);
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse archiveDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        requireDocumentAccess(document);

        if (document.getIsArchived()) {
            throw new BadRequestException("Document is already archived");
        }

        if (document.getLegalHold()) {
            throw new BadRequestException("Cannot archive document under legal hold");
        }

        document.setIsArchived(true);
        document.setArchivedAt(LocalDateTime.now());
        document = documentRepository.save(document);
        log.info("Document archived: {}", documentId);
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse setLegalHold(UUID documentId, boolean hold) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        requireDocumentAccess(document);

        document.setLegalHold(hold);
        document = documentRepository.save(document);
        log.info("Legal hold {} for document {}", hold ? "set" : "removed", documentId);
        return DocumentResponse.from(document);
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void processExpiredDocuments() {
        List<Document> expired = documentRepository.findExpiredDocuments(LocalDate.now());

        for (Document document : expired) {
            try {
                // Delete file from storage
                storageService.deleteFile(document.getFileKey());

                // Mark as archived
                document.setIsArchived(true);
                document.setArchivedAt(LocalDateTime.now());
                documentRepository.save(document);

                log.info("Expired document archived and file deleted: {} (retention ended {})",
                        document.getId(), document.getRetentionEndDate());
            } catch (Exception e) {
                log.error("Failed to process expired document {}: {}", document.getId(), e.getMessage());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Processed {} expired documents", expired.size());
        }
    }
}
