package com.howners.gestion.dto.listing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateListingRequest(
        @NotNull UUID propertyId,
        @NotBlank String title,
        String description,
        BigDecimal pricePerNight,
        BigDecimal pricePerMonth,
        String currency,
        Integer minStay,
        Integer maxStay,
        String amenities,
        String requirements,
        LocalDate availableFrom
) {
}
