package com.howners.gestion.dto.esignature;

import lombok.Builder;

/**
 * Requête pour créer une demande de signature électronique
 */
@Builder
public record ESignatureRequest(
        String documentName,
        byte[] documentContent,
        String signerEmail,
        String signerName,
        String returnUrl,
        Integer expirationDays
) {}
