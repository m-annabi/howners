package com.howners.gestion.dto.payments;

public record StripeConnectStatusResponse(
        boolean connected,
        String status,
        String onboardingUrl
) {}
