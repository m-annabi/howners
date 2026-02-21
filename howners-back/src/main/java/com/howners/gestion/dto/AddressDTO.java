package com.howners.gestion.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDTO(
        @NotBlank(message = "Address line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        String department,

        String country
) {
    public AddressDTO {
        if (country == null || country.isBlank()) {
            country = "FR";
        }
    }
}
