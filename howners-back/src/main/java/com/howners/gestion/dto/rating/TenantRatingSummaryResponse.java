package com.howners.gestion.dto.rating;

public record TenantRatingSummaryResponse(
        Double averagePaymentRating,
        Double averagePropertyRespectRating,
        Double averageCommunicationRating,
        Double averageOverallRating,
        Long totalRatings
) {}
