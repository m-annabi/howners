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
        String ownerName,
        String title,
        String description,
        BigDecimal pricePerNight,
        BigDecimal pricePerMonth,
        String currency,
        Integer minStay,
        Integer maxStay,
        ListingStatus status,
        String amenities,
        String requirements,
        LocalDate availableFrom,
        List<ListingPhotoResponse> photos,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    public static ListingResponse from(Listing l) {
        List<ListingPhotoResponse> photos = List.of();

        return new ListingResponse(
                l.getId(),
                l.getProperty().getId(),
                l.getProperty().getName(),
                l.getProperty().getCity(),
                l.getProperty().getPostalCode(),
                l.getProperty().getDepartment(),
                l.getProperty().getCountry(),
                l.getProperty().getOwner().getFullName(),
                l.getTitle(),
                l.getDescription(),
                l.getPricePerNight(),
                l.getPricePerMonth(),
                l.getCurrency(),
                l.getMinStay(),
                l.getMaxStay(),
                l.getStatus(),
                l.getAmenities(),
                l.getRequirements(),
                l.getAvailableFrom(),
                photos,
                l.getPublishedAt(),
                l.getCreatedAt()
        );
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
                l.getProperty().getOwner().getFullName(),
                l.getTitle(),
                l.getDescription(),
                l.getPricePerNight(),
                l.getPricePerMonth(),
                l.getCurrency(),
                l.getMinStay(),
                l.getMaxStay(),
                l.getStatus(),
                l.getAmenities(),
                l.getRequirements(),
                l.getAvailableFrom(),
                resolvedPhotos != null ? resolvedPhotos : List.of(),
                l.getPublishedAt(),
                l.getCreatedAt()
        );
    }
}
