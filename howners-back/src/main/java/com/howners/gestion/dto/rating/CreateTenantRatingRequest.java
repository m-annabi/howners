package com.howners.gestion.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTenantRatingRequest(
        @NotNull(message = "Tenant ID is required")
        UUID tenantId,

        UUID rentalId,

        @NotNull(message = "Payment rating is required")
        @Min(value = 1, message = "Payment rating must be between 1 and 5")
        @Max(value = 5, message = "Payment rating must be between 1 and 5")
        Integer paymentRating,

        @NotNull(message = "Property respect rating is required")
        @Min(value = 1, message = "Property respect rating must be between 1 and 5")
        @Max(value = 5, message = "Property respect rating must be between 1 and 5")
        Integer propertyRespectRating,

        @NotNull(message = "Communication rating is required")
        @Min(value = 1, message = "Communication rating must be between 1 and 5")
        @Max(value = 5, message = "Communication rating must be between 1 and 5")
        Integer communicationRating,

        String comment,

        String ratingPeriod
) {}
