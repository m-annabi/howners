package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de bienvenue d'un nouveau bailleur (compte OWNER).
 */
@Builder
public record WelcomeOwnerEmailData(
        String recipientEmail,
        String recipientName,
        String dashboardUrl,
        String addPropertyUrl,
        String pricingUrl
) {}
