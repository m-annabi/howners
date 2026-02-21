package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Données pour l'email de bienvenue d'un nouveau locataire
 */
@Builder
public record WelcomeTenantEmailData(
        String recipientEmail,
        String recipientName,
        String ownerName,
        String propertyName,
        String tempPassword,
        String loginUrl
) {}
