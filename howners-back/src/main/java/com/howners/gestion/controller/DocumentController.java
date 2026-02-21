package com.howners.gestion.controller;

import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.dto.document.SetLegalHoldRequest;
import com.howners.gestion.dto.document.SetRetentionRequest;
import com.howners.gestion.service.document.DocumentArchivingService;
import com.howners.gestion.service.document.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentArchivingService documentArchivingService;

    /**
     * POST /api/documents/upload - Upload un document
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "propertyId", required = false) UUID propertyId,
            @RequestParam(value = "rentalId", required = false) UUID rentalId,
            @RequestParam(value = "applicationId", required = false) UUID applicationId,
            @RequestParam(value = "description", required = false) String description) {

        try {
            log.info("Uploading document: {} of type {}", file.getOriginalFilename(), documentType);
            DocumentResponse document = documentService.uploadDocument(
                    file, documentType, propertyId, rentalId, applicationId, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (IOException e) {
            log.error("Error uploading document: {}", e.getMessage());
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    /**
     * GET /api/documents - Récupérer tous les documents de l'utilisateur
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {
        log.info("Fetching documents for current user");
        List<DocumentResponse> documents = documentService.getMyDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/{id} - Récupérer un document par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        log.info("Fetching document: {}", id);
        DocumentResponse document = documentService.getDocument(id);
        return ResponseEntity.ok(document);
    }

    /**
     * GET /api/documents/property/{propertyId} - Récupérer les documents d'une propriété
     */
    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getPropertyDocuments(@PathVariable UUID propertyId) {
        log.info("Fetching documents for property: {}", propertyId);
        List<DocumentResponse> documents = documentService.getPropertyDocuments(propertyId);
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/rental/{rentalId} - Récupérer les documents d'une location
     */
    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getRentalDocuments(@PathVariable UUID rentalId) {
        log.info("Fetching documents for rental: {}", rentalId);
        List<DocumentResponse> documents = documentService.getRentalDocuments(rentalId);
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/application/{applicationId} - Récupérer les documents d'une candidature
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getApplicationDocuments(@PathVariable UUID applicationId) {
        log.info("Fetching documents for application: {}", applicationId);
        List<DocumentResponse> documents = documentService.getApplicationDocuments(applicationId);
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/{id}/download - Télécharger un document
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        try {
            log.info("Downloading document: {}", id);
            DocumentResponse document = documentService.getDocument(id);
            byte[] data = documentService.downloadDocument(id);

            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                    .contentType(MediaType.parseMediaType(document.mimeType()))
                    .contentLength(data.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Error downloading document: {}", e.getMessage());
            throw new RuntimeException("Failed to download document", e);
        }
    }

    /**
     * DELETE /api/documents/{id} - Supprimer un document
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        log.info("Deleting document: {}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/documents/{id}/retention - Définir la période de rétention
     */
    @PutMapping("/{id}/retention")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<DocumentResponse> setRetention(
            @PathVariable UUID id,
            @Valid @RequestBody SetRetentionRequest request) {
        log.info("Setting retention for document {} until {}", id, request.retentionEndDate());
        DocumentResponse document = documentArchivingService.setRetentionPeriod(id, request.retentionEndDate());
        return ResponseEntity.ok(document);
    }

    /**
     * PUT /api/documents/{id}/archive - Archiver un document
     */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<DocumentResponse> archiveDocument(@PathVariable UUID id) {
        log.info("Archiving document: {}", id);
        DocumentResponse document = documentArchivingService.archiveDocument(id);
        return ResponseEntity.ok(document);
    }

    /**
     * PUT /api/documents/{id}/legal-hold - Mettre/retirer un blocage légal
     */
    @PutMapping("/{id}/legal-hold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> setLegalHold(
            @PathVariable UUID id,
            @Valid @RequestBody SetLegalHoldRequest request) {
        log.info("Setting legal hold for document {}: {}", id, request.hold());
        DocumentResponse document = documentArchivingService.setLegalHold(id, request.hold());
        return ResponseEntity.ok(document);
    }
}
