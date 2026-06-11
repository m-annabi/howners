package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Donnees pour l'email de relance d'onboarding (J+2) envoye aux bailleurs
 * qui n'ont pas encore ajoute de bien apres leur inscription.
 */
@Builder
public record OnboardingReminderEmailData(
        String recipientEmail,
        String recipientName,
        String addPropertyUrl,
        String dashboardUrl
) {}
