package com.howners.gestion.dto.esignature;

import lombok.Builder;

/**
 * Réponse après création d'une demande de signature électronique
 */
@Builder
public record ESignatureResponse(
        String envelopeId,
        String signingUrl,
        String status
) {}
