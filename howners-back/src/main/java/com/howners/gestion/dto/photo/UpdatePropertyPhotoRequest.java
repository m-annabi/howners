package com.howners.gestion.dto.photo;

public record UpdatePropertyPhotoRequest(
        String caption,
        Integer displayOrder,
        Boolean isPrimary
) {
}
