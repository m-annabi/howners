package com.howners.gestion.dto.rating;

import com.howners.gestion.domain.rating.TenantRating;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TenantRatingResponse(
        UUID id,
        UUID tenantId,
        String raterName,
        UUID rentalId,
        String propertyName,
        Integer paymentRating,
        Integer propertyRespectRating,
        Integer communicationRating,
        BigDecimal overallRating,
        String comment,
        LocalDateTime createdAt
) {
    public static TenantRatingResponse from(TenantRating r) {
        String propertyName = r.getRental() != null
                ? r.getRental().getProperty().getName()
                : null;
        return new TenantRatingResponse(
                r.getId(),
                r.getTenant().getId(),
                r.getRater().getFullName(),
                r.getRental() != null ? r.getRental().getId() : null,
                propertyName,
                r.getPaymentRating(),
                r.getPropertyRespectRating(),
                r.getCommunicationRating(),
                r.getOverallRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
