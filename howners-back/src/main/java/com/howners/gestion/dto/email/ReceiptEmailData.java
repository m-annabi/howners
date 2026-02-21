package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de notification de quittance de loyer
 */
@Builder
public record ReceiptEmailData(
        String recipientEmail,
        String recipientName,
        String ownerName,
        String propertyName,
        String propertyAddress,
        String receiptNumber,
        String periodLabel,
        String totalAmount,
        String currency,
        String receiptViewUrl
) {}
