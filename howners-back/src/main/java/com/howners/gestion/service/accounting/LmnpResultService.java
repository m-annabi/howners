package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.Loan;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.LoanRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Détermination du résultat fiscal LMNP au réel pour un exercice (année civile), en
 * base d'encaissement. Applique la règle de non-création de déficit par les
 * amortissements : l'annuité n'est déductible qu'à hauteur du bénéfice avant
 * amortissement, l'excédent devenant un amortissement différé reporté sans limite.
 */
@Service
@RequiredArgsConstructor
public class LmnpResultService {

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final AmortizableAssetRepository assetRepository;
    private final AmortizationService amortizationService;
    private final LoanRepository loanRepository;
    private final LoanScheduleService loanScheduleService;

    /** Catégories immobilisées (amorties, donc PAS charges de l'exercice). */
    private static final Map<ExpenseCategory, Boolean> CAPITALISEES = Map.of(
            ExpenseCategory.FURNISHING, true,
            ExpenseCategory.RENOVATION, true);

    private static final Map<ExpenseCategory, String> LIBELLES_CHARGES = new EnumMap<>(ExpenseCategory.class);
    static {
        LIBELLES_CHARGES.put(ExpenseCategory.MANAGEMENT_FEES, "Frais de gestion");
        LIBELLES_CHARGES.put(ExpenseCategory.LEGAL, "Frais juridiques et comptables");
        LIBELLES_CHARGES.put(ExpenseCategory.INSURANCE, "Assurances");
        LIBELLES_CHARGES.put(ExpenseCategory.MAINTENANCE, "Entretien");
        LIBELLES_CHARGES.put(ExpenseCategory.REPAIR, "Réparations");
        LIBELLES_CHARGES.put(ExpenseCategory.TAX, "Taxe foncière et taxes annexes");
        LIBELLES_CHARGES.put(ExpenseCategory.CONDO_FEES, "Charges de copropriété");
        LIBELLES_CHARGES.put(ExpenseCategory.UTILITIES, "Énergie et fluides");
        LIBELLES_CHARGES.put(ExpenseCategory.CLEANING, "Ménage");
        LIBELLES_CHARGES.put(ExpenseCategory.OTHER, "Autres charges");
    }

    public LmnpResult compute(FiscalActivity activity, int targetYear) {
        UUID ownerId = activity.getOwner().getId();
        List<Property> properties = propertyRepository.findByOwnerId(ownerId);
        List<AmortizableAsset> assets = assetRepository.findByActivityId(activity.getId());
        List<Expense> allExpenses = expenseRepository.findByOwnerId(ownerId);
        int startYear = activity.getStartDate().getYear();

        // Exercice antérieur au début d'activité : résultat vide (pas d'écritures).
        if (targetYear < startYear) {
            BigDecimal z = BigDecimal.ZERO;
            return new LmnpResult(targetYear, z, Map.of(), z, z, z, z, z, z, z, z, z,
                    z, z, z, z, z, z, z, List.of());
        }

        List<Loan> loans = loanRepository.findByActivityId(activity.getId());
        BigDecimal totalPrincipal = loans.stream().map(Loan::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal openingCash = nz(activity.getOpeningCash());
        BigDecimal baseImmo = assets.stream().map(AmortizableAsset::getBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Apport en nature (immobilisations) + numéraire − emprunts (financés par la dette).
        BigDecimal capitalExploitant = nz(activity.getApportInitial());
        if (capitalExploitant.signum() == 0) {
            capitalExploitant = openingCash.add(baseImmo).subtract(totalPrincipal);
        }

        // Cumuls séquentiels
        BigDecimal amortDiffereCumul = BigDecimal.ZERO;
        BigDecimal deficitCumul = BigDecimal.ZERO;
        BigDecimal reportANouveau = BigDecimal.ZERO; // Σ résultats comptables des exercices antérieurs
        BigDecimal tresorerie = openingCash;

        LmnpResult result = null;
        for (int y = startYear; y <= targetYear; y++) {
            final int exercice = y;
            BigDecimal recettes = recettes(properties, y);
            Map<String, BigDecimal> chargesParPoste = charges(allExpenses, y);

            // Charges financières déductibles : intérêts d'emprunt + assurance emprunteur.
            BigDecimal loanInterest = loans.stream().map(l -> loanScheduleService.forYear(l, exercice).interest())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal loanInsurance = loans.stream().map(l -> loanScheduleService.forYear(l, exercice).insurance())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal loanCapital = loans.stream().map(l -> loanScheduleService.forYear(l, exercice).capital())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (loanInterest.signum() > 0) chargesParPoste.merge("Intérêts d'emprunt", loanInterest, BigDecimal::add);
            if (loanInsurance.signum() > 0) chargesParPoste.merge("Assurance emprunteur", loanInsurance, BigDecimal::add);

            BigDecimal totalCharges = chargesParPoste.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal resultatAvantAmort = recettes.subtract(totalCharges);
            BigDecimal dotation = amortizationService.dotationExercice(assets, y);

            // Règle non-déficit : amortissement déductible = min(dotation + différé antérieur, bénéfice avant amort)
            BigDecimal amortDisponible = dotation.add(amortDiffereCumul);
            BigDecimal amortDeductible = resultatAvantAmort.signum() > 0
                    ? resultatAvantAmort.min(amortDisponible) : BigDecimal.ZERO;
            BigDecimal amortDiffereGenere = amortDisponible.subtract(amortDeductible);

            BigDecimal resultatComptable = resultatAvantAmort.subtract(dotation);
            BigDecimal resultatFiscalBrut = resultatAvantAmort.subtract(amortDeductible);

            // Déficit BIC reportable (imputation V1 non déduite du résultat courant, suivi séparé)
            BigDecimal resultatFiscal = resultatFiscalBrut;
            if (resultatFiscalBrut.signum() < 0) {
                deficitCumul = deficitCumul.add(resultatFiscalBrut.abs());
            }

            // Le capital remboursé n'est pas une charge mais sort de la trésorerie.
            tresorerie = tresorerie.add(recettes).subtract(totalCharges).subtract(loanCapital);

            if (y == targetYear) {
                BigDecimal immoBrutes = baseImmo;
                BigDecimal amortCumules = assets.stream()
                        .map(a -> amortizationService.cumul(a, targetYear))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal dettes = loans.stream()
                        .map(l -> loanScheduleService.crdEnd(l, targetYear))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<LmnpResult.AssetAmortLine> lignes = assets.stream()
                        .map(a -> new LmnpResult.AssetAmortLine(a, a.getBase(),
                                amortizationService.annuite(a, targetYear),
                                amortizationService.cumul(a, targetYear),
                                amortizationService.vnc(a, targetYear)))
                        .toList();

                result = new LmnpResult(y, recettes, chargesParPoste, totalCharges,
                        resultatAvantAmort, dotation, amortDeductible, amortDiffereGenere,
                        amortDiffereGenere, // stock d'amortissements différés en fin d'exercice
                        resultatComptable, resultatFiscal, deficitCumul,
                        immoBrutes, amortCumules, immoBrutes.subtract(amortCumules),
                        tresorerie, capitalExploitant, reportANouveau, dettes, lignes);
            }

            // Report vers l'exercice suivant
            amortDiffereCumul = amortDiffereGenere; // le stock devient l'excédent non déduit
            reportANouveau = reportANouveau.add(resultatComptable);
        }
        return result;
    }

    private BigDecimal recettes(List<Property> properties, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year + 1, 1, 1);
        BigDecimal total = BigDecimal.ZERO;
        for (Property p : properties) {
            BigDecimal r = paymentRepository.sumPaidRentByPropertyAndPeriod(
                    p.getId(), from.atStartOfDay(), to.atStartOfDay());
            if (r != null) total = total.add(r);
        }
        return total;
    }

    private Map<String, BigDecimal> charges(List<Expense> expenses, int year) {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        for (Expense e : expenses) {
            if (e.getExpenseDate() == null || e.getExpenseDate().getYear() != year) continue;
            if (Boolean.TRUE.equals(CAPITALISEES.get(e.getCategory()))) continue; // immobilisée
            String poste = LIBELLES_CHARGES.getOrDefault(e.getCategory(), "Autres charges");
            map.merge(poste, e.getAmount(), BigDecimal::add);
        }
        return map;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
