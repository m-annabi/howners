package com.howners.gestion.dto.esignature;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Événement webhook provenant du fournisseur de signature
 */
@Builder
public record WebhookEvent(
        String envelopeId,
        String eventType,
        String status,
        LocalDateTime eventDateTime,
        String signerEmail,
        String ipAddress,
        String userAgent
) {}
