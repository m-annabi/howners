package com.howners.gestion.dto.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.AssetType;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.service.accounting.LmnpResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTOs du module comptable (regroupés pour concision). */
public final class AccountingDtos {

    private AccountingDtos() {}

    public record ConfigureActivityRequest(
            LocalDate startDate,
            BigDecimal openingCash,
            String siret) {}

    public record CreateAssetRequest(
            AssetType type,
            String label,
            BigDecimal base,
            LocalDate startDate,
            Integer durationYears,
            UUID propertyId) {}

    public record ActivityResponse(
            UUID id, String jurisdiction, String regime, LocalDate startDate,
            BigDecimal openingCash, String siret, boolean active) {
        public static ActivityResponse from(FiscalActivity a) {
            return new ActivityResponse(a.getId(), a.getJurisdiction().name(), a.getRegime().name(),
                    a.getStartDate(), a.getOpeningCash(), a.getSiret(), Boolean.TRUE.equals(a.getActive()));
        }
    }

    public record AssetResponse(
            UUID id, String type, String typeLabel, String label, BigDecimal base,
            LocalDate startDate, Integer durationYears, UUID propertyId) {
        public static AssetResponse from(AmortizableAsset a) {
            return new AssetResponse(a.getId(), a.getType().name(), a.getType().getLabel(), a.getLabel(),
                    a.getBase(), a.getStartDate(), a.getDurationYears(),
                    a.getProperty() != null ? a.getProperty().getId() : null);
        }
    }

    public record ResultResponse(
            int year,
            BigDecimal recettes,
            Map<String, BigDecimal> chargesParPoste,
            BigDecimal totalCharges,
            BigDecimal resultatAvantAmortissement,
            BigDecimal dotationComptable,
            BigDecimal amortissementDeductible,
            BigDecimal amortissementDiffereCumul,
            BigDecimal resultatComptable,
            BigDecimal resultatFiscal,
            BigDecimal deficitAnterieurImpute,
            BigDecimal deficitReportable,
            BigDecimal vncImmobilisations,
            BigDecimal tresorerie,
            BigDecimal capitalExploitant,
            BigDecimal reportANouveau,
            BigDecimal dettesEmprunt,
            BigDecimal totalActif,
            BigDecimal totalPassif,
            List<AmortLineResponse> amortissements,
            List<String> avertissements) {
        public static ResultResponse from(LmnpResult r) {
            List<AmortLineResponse> lignes = r.lignesAmortissement().stream()
                    .map(l -> new AmortLineResponse(l.asset().getType().getLabel() + " — " + l.asset().getLabel(),
                            l.base(), l.annuite(), l.cumul(), l.vnc()))
                    .toList();
            return new ResultResponse(r.year(), r.recettes(), r.chargesParPoste(), r.totalCharges(),
                    r.resultatAvantAmortissement(), r.dotationComptable(), r.amortissementDeductible(),
                    r.amortissementDiffereCumul(), r.resultatComptable(), r.resultatFiscal(),
                    r.deficitAnterieurImpute(), r.deficitReportable(),
                    r.vncImmobilisations(), r.tresorerie(), r.capitalExploitant(),
                    r.reportANouveau(), r.dettesEmprunt(), r.totalActif(), r.totalPassif(), lignes,
                    r.avertissements());
        }
    }

    public record AmortLineResponse(String immobilisation, BigDecimal base, BigDecimal annuite,
                                    BigDecimal cumul, BigDecimal vnc) {}

    /** Immobilisation suggérée à partir d'une dépense ou d'un bien existant. */
    public record AssetSuggestion(
            String sourceType,   // EXPENSE | PROPERTY_BUILDING | PROPERTY_FEES
            UUID sourceId,
            AssetType type,
            String typeLabel,
            String label,
            BigDecimal base,
            LocalDate startDate,
            int durationYears) {}

    public record ImportSuggestionsRequest(List<ImportItem> items) {}

    public record ImportItem(String sourceType, UUID sourceId, Integer durationYears) {}

    public record CreateLoanRequest(
            String label,
            BigDecimal principal,
            BigDecimal annualRate,
            Integer durationMonths,
            LocalDate startDate,
            BigDecimal insuranceMonthly,
            UUID propertyId) {}

    public record LoanResponse(
            UUID id, String label, BigDecimal principal, BigDecimal annualRate,
            Integer durationMonths, LocalDate startDate, BigDecimal insuranceMonthly, UUID propertyId) {
        public static LoanResponse from(com.howners.gestion.domain.accounting.Loan l) {
            return new LoanResponse(l.getId(), l.getLabel(), l.getPrincipal(), l.getAnnualRate(),
                    l.getDurationMonths(), l.getStartDate(), l.getInsuranceMonthly(),
                    l.getProperty() != null ? l.getProperty().getId() : null);
        }
    }

    /** Une ligne de l'échéancier annuel d'un emprunt. */
    public record LoanYearResponse(int year, BigDecimal interest, BigDecimal capital,
                                   BigDecimal insurance, BigDecimal crdEnd) {}
}
