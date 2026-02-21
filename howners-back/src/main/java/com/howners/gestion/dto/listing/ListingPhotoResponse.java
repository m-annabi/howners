package com.howners.gestion.dto.listing;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListingPhotoResponse(
        UUID id,
        UUID listingId,
        String fileName,
        String fileUrl,
        Long fileSize,
        String mimeType,
        String caption,
        Integer displayOrder,
        Boolean isPrimary,
        UUID uploaderId,
        String uploaderName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
