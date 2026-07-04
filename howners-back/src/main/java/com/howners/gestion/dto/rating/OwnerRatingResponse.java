package com.howners.gestion.dto.rating;

import com.howners.gestion.domain.rating.OwnerRating;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OwnerRatingResponse(
        UUID id,
        UUID ownerId,
        String raterName,
        UUID rentalId,
        String propertyName,
        Integer communicationRating,
        Integer responsivenessRating,
        Integer contractRespectRating,
        BigDecimal overallRating,
        String comment,
        LocalDateTime createdAt
) {
    public static OwnerRatingResponse from(OwnerRating r) {
        String propertyName = r.getRental() != null
                ? r.getRental().getProperty().getName()
                : null;
        return new OwnerRatingResponse(
                r.getId(),
                r.getOwner().getId(),
                r.getRater().getFullName(),
                r.getRental() != null ? r.getRental().getId() : null,
                propertyName,
                r.getCommunicationRating(),
                r.getResponsivenessRating(),
                r.getContractRespectRating(),
                r.getOverallRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
