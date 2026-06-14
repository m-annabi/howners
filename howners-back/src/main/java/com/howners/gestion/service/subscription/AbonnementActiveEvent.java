package com.howners.gestion.service.subscription;

import com.howners.gestion.domain.subscription.PlanName;

import java.util.UUID;

/**
 * Publié quand un abonnement payant devient actif. premiereActivation = true uniquement
 * lors du tout premier passage à un plan payant (déclencheur des récompenses de parrainage).
 */
public record AbonnementActiveEvent(
        UUID userId,
        PlanName plan,
        boolean premiereActivation
) {
}
