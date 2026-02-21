package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de demande de signature
 */
@Builder
public record SignatureRequestEmailData(
        String recipientEmail,
        String recipientName,
        String ownerName,
        String propertyName,
        String propertyAddress,
        String contractNumber,
        String signingUrl,
        String expirationDate
) {}
