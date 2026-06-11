package com.howners.gestion.dto.email;

import lombok.Builder;

/**
 * Donnees pour l'email d'alerte d'echeance de contrat (J-30)
 * envoye au bailleur lorsqu'un contrat arrive a echeance dans 30 jours.
 */
@Builder
public record ContractExpiryEmailData(
        String recipientEmail,
        String recipientName,
        String tenantName,
        String propertyName,
        String contractNumber,
        String expiryDate,
        String contractViewUrl
) {}
