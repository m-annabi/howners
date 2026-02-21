package com.howners.gestion.dto.signature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSignatureRequest(
        @NotNull(message = "Contract ID is required")
        UUID contractId,

        @NotBlank(message = "Signature data is required")
        String signatureData,  // Base64 de l'image Canvas

        String ipAddress,

        String userAgent
) {
}
