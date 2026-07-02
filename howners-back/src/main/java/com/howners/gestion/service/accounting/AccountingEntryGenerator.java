package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.Loan;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.LoanRepository;
import com.howners.gestion.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Génère les écritures comptables en partie double d'un exercice à partir des
 * événements existants (loyers encaissés, dépenses, dotations aux amortissements) et
 * d'une écriture d'à-nouveau. Aucune saisie manuelle : la balance qui en résulte
 * alimente le bilan et le FEC, cohérents par construction (total débit = total crédit).
 */
@Service
@RequiredArgsConstructor
public class AccountingEntryGenerator {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final AmortizableAssetRepository assetRepository;
    private final AmortizationService amortizationService;
    private final LmnpResultService lmnpResultService;
    private final LoanRepository loanRepository;
    private final LoanScheduleService loanScheduleService;

    /** Une écriture (une ligne du FEC / du journal). */
    public record JournalEntry(LocalDate date, String journalCode, String journalLib,
                               String pieceRef, LocalDate pieceDate,
                               String compteNum, String compteLib, String libelle,
                               BigDecimal debit, BigDecimal credit) {}

    private static String chargeAccount(ExpenseCategory c) {
        return switch (c) {
            case TAX -> "63511";                 // Taxe foncière
            case INSURANCE -> "6161";            // Assurances
            case MANAGEMENT_FEES, LEGAL -> "622"; // Honoraires / gestion
            case CONDO_FEES -> "614";            // Charges de copropriété
            case UTILITIES -> "606";             // Énergie et fluides
            case MAINTENANCE, REPAIR -> "615";   // Entretien et réparations
            case CLEANING -> "6283";             // Nettoyage
            default -> "628";                    // Autres charges externes
        };
    }

    public List<JournalEntry> generate(FiscalActivity activity, int year) {
        List<JournalEntry> entries = new ArrayList<>();
        UUID ownerId = activity.getOwner().getId();
        int startYear = activity.getStartDate().getYear();
        LocalDate opening = LocalDate.of(year, 1, 1);

        // 1. À-nouveau (position d'ouverture de l'exercice)
        List<AmortizableAsset> assets = assetRepository.findByActivityId(activity.getId());
        BigDecimal immoBrutes = assets.stream().map(AmortizableAsset::getBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amortCumulOuverture = year > startYear
                ? assets.stream().map(a -> amortizationService.cumul(a, year - 1)).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        LmnpResult veille = year > startYear ? lmnpResultService.compute(activity, year - 1) : null;
        BigDecimal tresorerieOuverture = veille != null ? veille.tresorerie() : nz(activity.getOpeningCash());
        BigDecimal capital = veille != null ? veille.capitalExploitant() : capital(activity, immoBrutes);
        BigDecimal report = veille != null ? veille.reportANouveau().add(veille.resultatComptable()) : BigDecimal.ZERO;

        if (immoBrutes.signum() > 0) an(entries, opening, "211", "Immobilisations", immoBrutes, BigDecimal.ZERO);
        if (tresorerieOuverture.signum() != 0) an(entries, opening, "512", "Banque", tresorerieOuverture.max(BigDecimal.ZERO), tresorerieOuverture.signum() < 0 ? tresorerieOuverture.abs() : BigDecimal.ZERO);
        if (amortCumulOuverture.signum() > 0) an(entries, opening, "2811", "Amortissements", BigDecimal.ZERO, amortCumulOuverture);
        if (capital.signum() != 0) an(entries, opening, "108", "Compte de l'exploitant", BigDecimal.ZERO, capital);
        if (report.signum() != 0) an(entries, opening, "110", "Report à nouveau", report.signum() < 0 ? report.abs() : BigDecimal.ZERO, report.signum() > 0 ? report : BigDecimal.ZERO);

        // Emprunts : capital restant dû à l'ouverture (passif)
        List<Loan> loans = loanRepository.findByActivityId(activity.getId());
        BigDecimal crdOuverture = loans.stream()
                .map(l -> year > startYear ? loanScheduleService.crdEnd(l, year - 1) : l.getPrincipal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (crdOuverture.signum() > 0) an(entries, opening, "164", "Emprunts", BigDecimal.ZERO, crdOuverture);

        // 2. Loyers encaissés (journal de ventes)
        for (Payment p : paymentRepository.findByOwnerId(ownerId)) {
            if (p.getStatus() != PaymentStatus.PAID || p.getPaymentType() != PaymentType.RENT
                    || p.getPaidAt() == null || p.getPaidAt().getYear() != year) continue;
            LocalDate d = p.getPaidAt().toLocalDate();
            String ref = "LOY-" + p.getId().toString().substring(0, 8);
            entries.add(new JournalEntry(d, "VE", "Ventes", ref, d, "512", "Banque", "Loyer encaissé", p.getAmount(), BigDecimal.ZERO));
            entries.add(new JournalEntry(d, "VE", "Ventes", ref, d, "706", "Loyers", "Loyer encaissé", BigDecimal.ZERO, p.getAmount()));
        }

        // 3. Dépenses déductibles (journal d'achats)
        for (Expense e : expenseRepository.findByOwnerId(ownerId)) {
            if (e.getExpenseDate() == null || e.getExpenseDate().getYear() != year) continue;
            if (e.getCategory() == ExpenseCategory.FURNISHING || e.getCategory() == ExpenseCategory.RENOVATION) continue;
            LocalDate d = e.getExpenseDate();
            String ref = "DEP-" + e.getId().toString().substring(0, 8);
            String compte = chargeAccount(e.getCategory());
            entries.add(new JournalEntry(d, "AC", "Achats", ref, d, compte, "Charge " + e.getCategory().name(), nzLib(e.getDescription()), e.getAmount(), BigDecimal.ZERO));
            entries.add(new JournalEntry(d, "AC", "Achats", ref, d, "512", "Banque", nzLib(e.getDescription()), BigDecimal.ZERO, e.getAmount()));
        }

        // 3b. Remboursements d'emprunt (capital + intérêts + assurance)
        LocalDate echeance = LocalDate.of(year, 12, 31);
        for (Loan l : loans) {
            var ly = loanScheduleService.forYear(l, year);
            if (ly.capital().signum() <= 0 && ly.interest().signum() <= 0) continue;
            String ref = "EMP-" + l.getId().toString().substring(0, 8);
            BigDecimal total = ly.capital().add(ly.interest()).add(ly.insurance());
            if (ly.capital().signum() > 0) entries.add(new JournalEntry(echeance, "OD", "Emprunts", ref, echeance, "164", "Emprunts", "Remboursement capital " + l.getLabel(), ly.capital(), BigDecimal.ZERO));
            if (ly.interest().signum() > 0) entries.add(new JournalEntry(echeance, "OD", "Emprunts", ref, echeance, "66116", "Intérêts d'emprunt", "Intérêts " + l.getLabel(), ly.interest(), BigDecimal.ZERO));
            if (ly.insurance().signum() > 0) entries.add(new JournalEntry(echeance, "OD", "Emprunts", ref, echeance, "6162", "Assurance emprunteur", "Assurance " + l.getLabel(), ly.insurance(), BigDecimal.ZERO));
            entries.add(new JournalEntry(echeance, "OD", "Emprunts", ref, echeance, "512", "Banque", "Échéance " + l.getLabel(), BigDecimal.ZERO, total));
        }

        // 4. Dotations aux amortissements (opérations diverses, à la clôture)
        LocalDate cloture = LocalDate.of(year, 12, 31);
        for (AmortizableAsset a : assets) {
            BigDecimal annuite = amortizationService.annuite(a, year);
            if (annuite.signum() <= 0) continue;
            String ref = "AMORT-" + a.getId().toString().substring(0, 8);
            entries.add(new JournalEntry(cloture, "OD", "Opérations diverses", ref, cloture, "6811", "Dotations aux amortissements", a.getLabel(), annuite, BigDecimal.ZERO));
            entries.add(new JournalEntry(cloture, "OD", "Opérations diverses", ref, cloture, "2811", "Amortissements", a.getLabel(), BigDecimal.ZERO, annuite));
        }

        return entries;
    }

    private void an(List<JournalEntry> entries, LocalDate d, String compte, String lib, BigDecimal debit, BigDecimal credit) {
        entries.add(new JournalEntry(d, "AN", "À-nouveaux", "AN-" + d.getYear(), d, compte, lib, "Report à nouveau", debit, credit));
    }

    private BigDecimal capital(FiscalActivity activity, BigDecimal immoBrutes) {
        BigDecimal apport = nz(activity.getApportInitial());
        if (apport.signum() != 0) return apport;
        BigDecimal principal = loanRepository.findByActivityId(activity.getId()).stream()
                .map(Loan::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return nz(activity.getOpeningCash()).add(immoBrutes).subtract(principal);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static String nzLib(String s) { return s != null && !s.isBlank() ? s : "-"; }
}
