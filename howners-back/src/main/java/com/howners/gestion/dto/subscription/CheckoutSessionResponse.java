package com.howners.gestion.dto.subscription;

public record CheckoutSessionResponse(
        String sessionId,
        String checkoutUrl
) {}
