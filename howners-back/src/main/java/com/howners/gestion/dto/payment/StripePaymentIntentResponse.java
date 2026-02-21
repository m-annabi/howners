package com.howners.gestion.dto.payment;

public record StripePaymentIntentResponse(
        String clientSecret,
        String paymentIntentId,
        String status
) {}
