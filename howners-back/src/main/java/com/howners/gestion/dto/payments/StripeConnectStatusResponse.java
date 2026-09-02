package com.howners.gestion.dto.payments;

public record StripeConnectStatusResponse(
        boolean connected,
        String status,
        String onboardingUrl,
        String paymentInstructions,
        boolean acceptOnlinePayments,
        /** Jour d'envoi des quittances (null = immédiat). */
        Integer receiptSendDay,
        /** Commission plateforme (%) appliquée aux paiements carte de ce bailleur. */
        java.math.BigDecimal platformFeePercent
) {}
