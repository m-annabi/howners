package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.Loan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Tableau d'amortissement d'un emprunt à échéance constante. Fournit, par année civile,
 * la ventilation intérêts / capital / assurance et le capital restant dû en fin d'année.
 * En LMNP réel, seuls les intérêts et l'assurance sont déductibles.
 */
@Service
public class LoanScheduleService {

    /** Ventilation annuelle d'un emprunt. */
    public record LoanYear(int year, BigDecimal interest, BigDecimal capital,
                           BigDecimal insurance, BigDecimal crdEnd) {}

    /** Ventilation par année civile sur toute la durée de l'emprunt. */
    public Map<Integer, LoanYear> yearlyBreakdown(Loan loan) {
        Map<Integer, BigDecimal> interestByYear = new HashMap<>();
        Map<Integer, BigDecimal> capitalByYear = new HashMap<>();
        Map<Integer, BigDecimal> insuranceByYear = new HashMap<>();
        Map<Integer, BigDecimal> crdEndByYear = new HashMap<>();

        int n = loan.getDurationMonths();
        BigDecimal principal = loan.getPrincipal();
        double r = loan.getAnnualRate().doubleValue() / 100.0 / 12.0;
        BigDecimal insurance = loan.getInsuranceMonthly() != null ? loan.getInsuranceMonthly() : BigDecimal.ZERO;

        // Mensualité constante (hors assurance)
        double payment = r > 0
                ? principal.doubleValue() * r / (1 - Math.pow(1 + r, -n))
                : principal.doubleValue() / n;

        BigDecimal crd = principal;
        for (int m = 0; m < n; m++) {
            LocalDate date = loan.getStartDate().plusMonths(m);
            int year = date.getYear();
            BigDecimal interest = crd.multiply(BigDecimal.valueOf(r)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital = BigDecimal.valueOf(payment).subtract(interest).setScale(2, RoundingMode.HALF_UP);
            if (m == n - 1 || capital.compareTo(crd) > 0) {
                capital = crd; // dernière échéance : solde le capital restant
            }
            crd = crd.subtract(capital);

            interestByYear.merge(year, interest, BigDecimal::add);
            capitalByYear.merge(year, capital, BigDecimal::add);
            insuranceByYear.merge(year, insurance, BigDecimal::add);
            crdEndByYear.put(year, crd);
        }

        Map<Integer, LoanYear> result = new HashMap<>();
        for (Integer y : interestByYear.keySet()) {
            result.put(y, new LoanYear(y,
                    interestByYear.getOrDefault(y, BigDecimal.ZERO),
                    capitalByYear.getOrDefault(y, BigDecimal.ZERO),
                    insuranceByYear.getOrDefault(y, BigDecimal.ZERO),
                    crdEndByYear.getOrDefault(y, BigDecimal.ZERO)));
        }
        return result;
    }

    /** Ventilation d'une année (zéros si l'emprunt n'est pas actif cette année-là). */
    public LoanYear forYear(Loan loan, int year) {
        LoanYear y = yearlyBreakdown(loan).get(year);
        if (y != null) return y;
        // Avant le début : CRD = principal ; après la fin : CRD = 0.
        BigDecimal crd = year < loan.getStartDate().getYear() ? loan.getPrincipal() : BigDecimal.ZERO;
        return new LoanYear(year, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, crd);
    }

    /** Capital restant dû à la fin de l'année (pour le bilan). */
    public BigDecimal crdEnd(Loan loan, int year) {
        return forYear(loan, year).crdEnd();
    }
}
