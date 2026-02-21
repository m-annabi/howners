package com.howners.gestion.dto.subscription;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCheckoutSessionRequest(
        @NotNull UUID planId,
        String billingPeriod // "monthly" or "annual"
) {}
