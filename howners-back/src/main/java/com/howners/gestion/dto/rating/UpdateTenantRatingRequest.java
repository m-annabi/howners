package com.howners.gestion.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateTenantRatingRequest(
        @Min(value = 1, message = "Payment rating must be between 1 and 5")
        @Max(value = 5, message = "Payment rating must be between 1 and 5")
        Integer paymentRating,

        @Min(value = 1, message = "Property respect rating must be between 1 and 5")
        @Max(value = 5, message = "Property respect rating must be between 1 and 5")
        Integer propertyRespectRating,

        @Min(value = 1, message = "Communication rating must be between 1 and 5")
        @Max(value = 5, message = "Communication rating must be between 1 and 5")
        Integer communicationRating,

        String comment,

        String ratingPeriod
) {}
