package com.howners.gestion.dto.listing;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String propertyCity,
        String propertyPostalCode,
        String propertyDepartment,
        String propertyCountry,
        BigDecimal propertyLatitude,
        BigDecimal propertyLongitude,
        BigDecimal propertySurface,
        Integer propertyBedrooms,
        Boolean propertyFurnished,
        // Id du bailleur : requis par « Contacter le propriétaire » (ouvre la conversation) et
        // par la détection isOwner côté fiche annonce.
        UUID ownerId,
        String ownerName,
        String title,
        String description,
        BigDecimal pricePerNight,
        BigDecimal pricePerMonth,
        String currency,
        Integer minStay,
        Integer maxStay,
        ListingStatus status,
        java.util.List<String> amenities,
        java.util.List<String> requirements,
        LocalDate availableFrom,
        List<ListingPhotoResponse> photos,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    public static ListingResponse from(Listing l) {
        return from(l, List.of());
    }

    public static ListingResponse from(Listing l, List<ListingPhotoResponse> resolvedPhotos) {
        return new ListingResponse(
                l.getId(),
                l.getProperty().getId(),
                l.getProperty().getName(),
                l.getProperty().getCity(),
                l.getProperty().getPostalCode(),
                l.getProperty().getDepartment(),
                l.getProperty().getCountry(),
                l.getProperty().getLatitude(),
                l.getProperty().getLongitude(),
                l.getProperty().getSurfaceArea(),
                l.getProperty().getBedrooms(),
                l.getProperty().getIsFurnished(),
                l.getProperty().getOwner().getId(),
                l.getProperty().getOwner().getFullName(),
                l.getTitle(),
                l.getDescription(),
                l.getPricePerNight(),
                l.getPricePerMonth(),
                l.getCurrency(),
                l.getMinStay(),
                l.getMaxStay(),
                l.getStatus(),
                parseJsonList(l.getAmenities()),
                parseJsonList(l.getRequirements()),
                l.getAvailableFrom(),
                resolvedPhotos != null ? resolvedPhotos : List.of(),
                l.getPublishedAt(),
                l.getCreatedAt()
        );
    }

    /** Les listes sont stockées en JSON (["wifi","parking"]) ; les anciennes valeurs texte deviennent une liste à un élément. */
    private static java.util.List<String> parseJsonList(String raw) {
        if (raw == null || raw.isBlank()) return java.util.List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
        } catch (Exception e) {
            return java.util.List.of(raw);
        }
    }
}
