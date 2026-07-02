package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Résultat fiscal LMNP réel d'un exercice, avec les données nécessaires au compte de
 * résultat, au bilan simplifié et au tableau des amortissements.
 */
public record LmnpResult(
        int year,
        // Compte de résultat
        BigDecimal recettes,
        Map<String, BigDecimal> chargesParPoste,
        BigDecimal totalCharges,
        BigDecimal resultatAvantAmortissement,
        BigDecimal dotationComptable,     // annuités de l'exercice (comptable)
        BigDecimal amortissementDeductible, // fraction fiscalement déduite (règle non-déficit)
        BigDecimal amortissementDiffereGenere, // excédent reporté au titre de l'exercice
        BigDecimal amortissementDiffereCumul,  // stock d'amortissements différés en fin d'exercice
        BigDecimal resultatComptable,     // recettes - charges - dotation comptable
        BigDecimal resultatFiscal,        // base imposable (après non-déficit et déficits antérieurs)
        BigDecimal deficitAnterieurImpute, // déficits BIC antérieurs imputés sur l'exercice
        BigDecimal deficitReportable,     // déficits BIC restant reportables (10 ans)
        // Bilan simplifié
        BigDecimal immobilisationsBrutes,
        BigDecimal amortissementsCumules,
        BigDecimal vncImmobilisations,
        BigDecimal tresorerie,
        BigDecimal capitalExploitant,
        BigDecimal reportANouveau,
        BigDecimal dettesEmprunt,       // capital restant dû des emprunts (passif)
        // Détail amortissements (par immobilisation, pour le tableau)
        List<AssetAmortLine> lignesAmortissement,
        // Points d'attention (biens non classés meublé/nu, seuil LMP, double déduction…)
        List<String> avertissements
) implements FiscalResult {

    public record AssetAmortLine(AmortizableAsset asset, BigDecimal base, BigDecimal annuite,
                                 BigDecimal cumul, BigDecimal vnc) {}

    public BigDecimal totalActif() {
        return vncImmobilisations.add(tresorerie);
    }

    public BigDecimal totalPassif() {
        return capitalExploitant.add(reportANouveau).add(resultatComptable).add(dettesEmprunt);
    }
}
