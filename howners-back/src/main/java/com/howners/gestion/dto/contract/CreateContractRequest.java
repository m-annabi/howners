package com.howners.gestion.dto.contract;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateContractRequest(
        @NotNull(message = "Rental ID is required")
        UUID rentalId,

        UUID templateId,  // Si null, utilise le template par défaut

        @Size(max = 51200, message = "Custom content must not exceed 50KB")
        String customContent  // Contenu personnalisé pré-rempli (optionnel)
) {
}
