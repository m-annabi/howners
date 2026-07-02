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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Détermination du résultat fiscal LMNP au réel pour un exercice (année civile), en
 * base d'encaissement, sur les seuls biens meublés. Applique la règle de non-création
 * de déficit par les amortissements (excédent différé sans limite, art. 39 C CGI) puis
 * l'imputation des déficits BIC non professionnels antérieurs (report 10 ans, FIFO).
 */
@Service
@RequiredArgsConstructor
public class LmnpResultService {

    /** Recettes au-delà desquelles le statut LMP peut s'appliquer (art. 155 IV CGI). */
    public static final BigDecimal SEUIL_LMP = new BigDecimal("23000");
    /** Durée de report des déficits BIC non professionnels (années). */
    private static final int REPORT_DEFICIT_ANNEES = 10;

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

    /** Lot de déficit BIC reportable (année d'origine, montant restant). */
    private static final class DeficitLot {
        final int origine;
        BigDecimal restant;
        DeficitLot(int origine, BigDecimal restant) { this.origine = origine; this.restant = restant; }
    }

    /** Le BIC LMNP ne porte que sur les biens meublés (les biens nus relèvent de la 2044). */
    static boolean estMeuble(Property p) {
        return !Boolean.FALSE.equals(p.getIsFurnished());
    }

    public LmnpResult compute(FiscalActivity activity, int targetYear) {
        UUID ownerId = activity.getOwner().getId();
        List<Property> properties = propertyRepository.findByOwnerId(ownerId).stream()
                .filter(LmnpResultService::estMeuble).toList();
        List<AmortizableAsset> assets = assetRepository.findByActivityId(activity.getId());
        List<Expense> allExpenses = expenseRepository.findByOwnerId(ownerId);
        int startYear = activity.getStartDate().getYear();

        // Exercice antérieur au début d'activité : résultat vide (pas d'écritures).
        if (targetYear < startYear) {
            BigDecimal z = BigDecimal.ZERO;
            return new LmnpResult(targetYear, z, Map.of(), z, z, z, z, z, z, z, z, z, z,
                    z, z, z, z, z, z, z, List.of(), List.of());
        }

        List<Loan> loans = loanRepository.findByActivityId(activity.getId());
        // Emprunts « initiaux » (au plus tard l'année du début d'activité) : ils financent
        // le patrimoine apporté et réduisent le capital de l'exploitant. Les emprunts
        // postérieurs injectent leur capital en trésorerie l'année du déblocage.
        BigDecimal detteOuverture = loans.stream()
                .filter(l -> l.getStartDate().getYear() <= startYear)
                .map(l -> l.getStartDate().getYear() == startYear
                        ? l.getPrincipal() : loanScheduleService.crdEnd(l, startYear - 1))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal openingCash = nz(activity.getOpeningCash());
        BigDecimal baseImmo = assets.stream().map(AmortizableAsset::getBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Apport en nature (immobilisations) + numéraire − dette d'ouverture.
        BigDecimal capitalExploitant = openingCash.add(baseImmo).subtract(detteOuverture);

        // Cumuls séquentiels
        BigDecimal amortDiffereCumul = BigDecimal.ZERO;
        List<DeficitLot> deficits = new ArrayList<>();
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
            BigDecimal amortDiffereFin = amortDisponible.subtract(amortDeductible);
            BigDecimal amortDiffereGenere = dotation.subtract(amortDeductible).max(BigDecimal.ZERO);

            BigDecimal resultatComptable = resultatAvantAmort.subtract(dotation);
            BigDecimal resultatFiscalBrut = resultatAvantAmort.subtract(amortDeductible);

            // Déficits BIC non professionnels : péremption à 10 ans puis imputation FIFO.
            deficits.removeIf(lot -> exercice > lot.origine + REPORT_DEFICIT_ANNEES);
            BigDecimal deficitImpute = BigDecimal.ZERO;
            BigDecimal resultatFiscal = resultatFiscalBrut;
            if (resultatFiscalBrut.signum() > 0) {
                for (DeficitLot lot : deficits) {
                    if (resultatFiscal.signum() <= 0) break;
                    BigDecimal part = lot.restant.min(resultatFiscal);
                    lot.restant = lot.restant.subtract(part);
                    resultatFiscal = resultatFiscal.subtract(part);
                    deficitImpute = deficitImpute.add(part);
                }
                deficits.removeIf(lot -> lot.restant.signum() <= 0);
            } else if (resultatFiscalBrut.signum() < 0) {
                deficits.add(new DeficitLot(exercice, resultatFiscalBrut.abs()));
            }
            BigDecimal deficitReportable = deficits.stream().map(l -> l.restant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Trésorerie : le capital remboursé n'est pas une charge mais sort de la caisse ;
            // un emprunt débloqué en cours d'activité y entre l'année de son déblocage.
            BigDecimal debloque = loans.stream()
                    .filter(l -> l.getStartDate().getYear() == exercice && exercice > startYear)
                    .map(Loan::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
            tresorerie = tresorerie.add(recettes).subtract(totalCharges).subtract(loanCapital).add(debloque);

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
                        amortDiffereFin, resultatComptable, resultatFiscal,
                        deficitImpute, deficitReportable,
                        immoBrutes, amortCumules, immoBrutes.subtract(amortCumules),
                        tresorerie, capitalExploitant, reportANouveau, dettes, lignes,
                        avertissements(ownerId, recettes, loans));
            }

            // Report vers l'exercice suivant
            amortDiffereCumul = amortDiffereFin;
            reportANouveau = reportANouveau.add(resultatComptable);
        }
        return result;
    }

    private List<String> avertissements(UUID ownerId, BigDecimal recettes, List<Loan> loans) {
        List<String> notes = new ArrayList<>();
        List<String> nonClasses = propertyRepository.findByOwnerId(ownerId).stream()
                .filter(p -> p.getIsFurnished() == null)
                .map(Property::getName).toList();
        if (!nonClasses.isEmpty()) {
            notes.add("Biens non classés meublé / nu, inclus dans le BIC : "
                    + String.join(", ", nonClasses)
                    + ". Renseignez le caractère meublé de chaque bien pour éviter tout double emploi avec la déclaration 2044 (location nue).");
        }
        if (recettes.compareTo(SEUIL_LMP) > 0) {
            notes.add("Les recettes de l'exercice dépassent 23 000 € : si elles excèdent aussi les autres revenus d'activité du foyer, "
                    + "l'activité bascule en loueur en meublé professionnel (LMP) — cotisations sociales et régime des plus-values différents.");
        }
        if (!loans.isEmpty()) {
            notes.add("Emprunt(s) modélisé(s) : ne saisissez pas en plus les intérêts ou l'assurance emprunteur en dépenses, ils seraient déduits deux fois.");
        }
        return notes;
    }

    private BigDecimal recettes(List<Property> properties, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year + 1, 1, 1);
        BigDecimal total = BigDecimal.ZERO;
        for (Property p : properties) {
            BigDecimal r = paymentRepository.sumPaidRentAndChargesByPropertyAndPeriod(
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
            if (e.getProperty() != null && !estMeuble(e.getProperty())) continue; // bien nu → 2044
            String poste = LIBELLES_CHARGES.getOrDefault(e.getCategory(), "Autres charges");
            map.merge(poste, e.getAmount(), BigDecimal::add);
        }
        return map;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
