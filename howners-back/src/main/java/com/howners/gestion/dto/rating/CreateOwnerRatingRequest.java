package com.howners.gestion.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOwnerRatingRequest(
        @NotNull UUID ownerId,
        UUID rentalId,
        @NotNull @Min(1) @Max(5) Integer communicationRating,
        @NotNull @Min(1) @Max(5) Integer responsivenessRating,
        @NotNull @Min(1) @Max(5) Integer contractRespectRating,
        String comment
) {}
