package com.howners.gestion.dto.rating;

import com.howners.gestion.domain.rating.TenantRating;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantRatingResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        UUID raterId,
        String raterName,
        UUID rentalId,
        String propertyName,
        Integer paymentRating,
        Integer propertyRespectRating,
        Integer communicationRating,
        Double overallRating,
        String comment,
        String ratingPeriod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TenantRatingResponse from(TenantRating rating) {
        return new TenantRatingResponse(
                rating.getId(),
                rating.getTenant().getId(),
                rating.getTenant().getFullName(),
                rating.getRater().getId(),
                rating.getRater().getFullName(),
                rating.getRental() != null ? rating.getRental().getId() : null,
                rating.getRental() != null ? rating.getRental().getProperty().getName() : null,
                rating.getPaymentRating(),
                rating.getPropertyRespectRating(),
                rating.getCommunicationRating(),
                rating.getOverallRating(),
                rating.getComment(),
                rating.getRatingPeriod(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}
