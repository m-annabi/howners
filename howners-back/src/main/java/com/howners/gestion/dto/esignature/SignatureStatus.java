package com.howners.gestion.dto.esignature;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Statut d'une signature électronique
 */
@Builder
public record SignatureStatus(
        String envelopeId,
        String status,
        LocalDateTime sentDateTime,
        LocalDateTime completedDateTime,
        String signerEmail
) {}
