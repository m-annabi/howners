package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de confirmation de signature
 */
@Builder
public record SignatureCompletedEmailData(
        String recipientEmail,
        String recipientName,
        String tenantName,
        String propertyName,
        String contractNumber,
        String signedDate,
        String contractViewUrl
) {}
