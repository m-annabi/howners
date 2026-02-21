package com.howners.gestion.dto.template;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PreviewTemplateRequest(
        @NotNull(message = "Rental ID is required")
        UUID rentalId,

        @Size(max = 51200, message = "Custom content must not exceed 50KB")
        String customContent
) {
}
