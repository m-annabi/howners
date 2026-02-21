package com.howners.gestion.dto.photo;

import com.howners.gestion.domain.photo.PropertyPhoto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyPhotoResponse(
        UUID id,
        UUID propertyId,
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
    public static PropertyPhotoResponse from(PropertyPhoto photo) {
        return new PropertyPhotoResponse(
                photo.getId(),
                photo.getProperty().getId(),
                photo.getFileName(),
                photo.getFileUrl(),
                photo.getFileSize(),
                photo.getMimeType(),
                photo.getCaption(),
                photo.getDisplayOrder(),
                photo.getIsPrimary(),
                photo.getUploader().getId(),
                getFullName(photo.getUploader()),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
