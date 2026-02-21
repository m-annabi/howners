package com.howners.gestion.dto.document;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String fileUrl,
        Long fileSize,
        String mimeType,
        DocumentType documentType,
        UUID propertyId,
        UUID rentalId,
        UUID applicationId,
        UUID uploaderId,
        String uploaderName,
        String documentHash,
        String description,
        LocalDateTime uploadedAt,
        LocalDate retentionEndDate,
        LocalDateTime archivedAt,
        Boolean isArchived,
        Boolean legalHold
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getFileUrl(),
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

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
