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
        List<String> avertissements,
        // Checklist « prêt à déposer » : état de complétude et actions guidées avant dépôt.
        List<ReadinessCheck> checklist,
        // Aide au report : cases à servir sur la déclaration (2031 / 2033 / 2042-C-PRO).
        List<ReportLine> reportLines
) implements FiscalResult {

    public record AssetAmortLine(AmortizableAsset asset, BigDecimal base, BigDecimal annuite,
                                 BigDecimal cumul, BigDecimal vnc) {}

    /**
     * Une vérification de complétude avant dépôt. {@code level} : {@code DONE} (déjà en
     * ordre), {@code ACTION} (à traiter avant de déclarer), {@code INFO} (repère utile).
     */
    public record ReadinessCheck(String level, String titre, String detail) {
        public static ReadinessCheck done(String titre, String detail) { return new ReadinessCheck("DONE", titre, detail); }
        public static ReadinessCheck action(String titre, String detail) { return new ReadinessCheck("ACTION", titre, detail); }
        public static ReadinessCheck info(String titre, String detail) { return new ReadinessCheck("INFO", titre, detail); }
    }

    /** Une ligne d'aide au report : un montant et la case/rubrique où l'inscrire. */
    public record ReportLine(String libelle, BigDecimal montant, String destination) {}

    /** Vrai si aucune action bloquante ne reste avant de déposer la déclaration. */
    public boolean pretADeposer() {
        return checklist != null && checklist.stream().noneMatch(c -> "ACTION".equals(c.level()));
    }

    public BigDecimal totalActif() {
        return vncImmobilisations.add(tresorerie);
    }

    public BigDecimal totalPassif() {
        return capitalExploitant.add(reportANouveau).add(resultatComptable).add(dettesEmprunt);
    }
}
