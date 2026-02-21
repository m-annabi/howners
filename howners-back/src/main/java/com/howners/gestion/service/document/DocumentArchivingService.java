package com.howners.gestion.service.document;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
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
    private final StorageService storageService;

    @Transactional
    public DocumentResponse setRetentionPeriod(UUID documentId, LocalDate retentionEndDate) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

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
