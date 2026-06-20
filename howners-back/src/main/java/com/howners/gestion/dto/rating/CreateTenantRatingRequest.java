package com.howners.gestion.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTenantRatingRequest(
        @NotNull UUID tenantId,
        UUID rentalId,
        @NotNull @Min(1) @Max(5) Integer paymentRating,
        @NotNull @Min(1) @Max(5) Integer propertyRespectRating,
        @NotNull @Min(1) @Max(5) Integer communicationRating,
        String comment
) {}
