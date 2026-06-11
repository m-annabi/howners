package com.howners.gestion.dto.listing;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateListingRequest(
        @NotNull UUID propertyId,
        @NotBlank @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères") String title,
        @Size(max = 5000, message = "La description ne doit pas dépasser 5000 caractères") String description,
        @DecimalMin(value = "0.0", message = "Le prix par nuit ne peut pas être négatif") BigDecimal pricePerNight,
        @DecimalMin(value = "0.0", message = "Le prix mensuel ne peut pas être négatif") BigDecimal pricePerMonth,
        String currency,
        @Min(value = 1, message = "La durée minimum doit être d'au moins 1 jour") Integer minStay,
        Integer maxStay,
        String amenities,
        String requirements,
        LocalDate availableFrom
) {
}
