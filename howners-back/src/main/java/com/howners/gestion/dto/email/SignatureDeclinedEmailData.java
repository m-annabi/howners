package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de refus de signature
 */
@Builder
public record SignatureDeclinedEmailData(
        String recipientEmail,
        String recipientName,
        String tenantName,
        String propertyName,
        String contractNumber,
        String declinedDate,
        String reason
) {}
