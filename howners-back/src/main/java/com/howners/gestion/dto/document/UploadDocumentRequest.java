package com.howners.gestion.dto.document;

import com.howners.gestion.domain.document.DocumentType;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record UploadDocumentRequest(
        @NotNull(message = "File is required")
        MultipartFile file,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        UUID propertyId,

        UUID rentalId,

        String description
) {
}
