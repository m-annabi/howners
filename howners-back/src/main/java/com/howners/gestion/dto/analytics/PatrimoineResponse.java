package com.howners.gestion.dto.analytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vue patrimoniale du portefeuille : rentabilité, cash-flow et occupation
 * calculés sur les 12 derniers mois glissants.
 */
public record PatrimoineResponse(
        List<BienPatrimoine> biens,
        BigDecimal valeurAchatTotale,
        BigDecimal revenusAnnuelsTotaux,
        BigDecimal chargesAnnuellesTotales,
        BigDecimal cashFlowMensuelTotal,
        BigDecimal rendementNetMoyenPondere
) {
    public record BienPatrimoine(
            UUID propertyId,
            String nom,
            String ville,
            BigDecimal purchasePrice,
            BigDecimal revenusAnnuels,
            BigDecimal chargesAnnuelles,
            BigDecimal cashFlowMensuel,
            BigDecimal rentabiliteBrutePercent,
            BigDecimal rentabiliteNettePercent,
            BigDecimal tauxOccupationPercent
    ) {}
}
