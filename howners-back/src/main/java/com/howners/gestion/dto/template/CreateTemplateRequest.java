package com.howners.gestion.dto.template;

import com.howners.gestion.domain.rental.RentalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(
        @NotBlank(message = "Template name is required")
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Rental type is required")
        RentalType rentalType,

        @NotBlank(message = "Template content is required")
        @Size(max = 51200, message = "Template content must not exceed 50KB")
        String content
) {
}
