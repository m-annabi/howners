package com.howners.gestion.dto.analytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrégats d'aide à la déclaration des revenus fonciers (formulaire 2044, régime réel).
 * Document d'aide : ne remplace pas la déclaration officielle.
 */
public record Declaration2044Response(
        int annee,
        List<BienDeclaration> biens,
        Map<String, BigDecimal> totauxParLigne,
        BigDecimal totalRevenusBruts,
        BigDecimal totalChargesDeductibles,
        BigDecimal revenuFoncierNet
) {
    public record BienDeclaration(
            UUID propertyId,
            String nom,
            String adresse,
            BigDecimal revenusBruts,
            Map<String, BigDecimal> chargesParLigne,
            BigDecimal totalCharges,
            BigDecimal revenuNet
    ) {}
}
