package com.howners.gestion.dto.contract;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête de signature canvas (HTML5) via le flow public tokenisé.
 * Le champ signatureData doit contenir l'image PNG en base64 (data:image/png;base64,...).
 */
public record CanvasSignRequest(
        @NotBlank(message = "Signature data is required")
        String signatureData
) {}
