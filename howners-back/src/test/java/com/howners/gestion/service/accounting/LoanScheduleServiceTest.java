package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.Loan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoanScheduleServiceTest {

    private final LoanScheduleService service = new LoanScheduleService();

    private Loan loan(String principal, String rate, int months, LocalDate start) {
        return Loan.builder().principal(new BigDecimal(principal)).annualRate(new BigDecimal(rate))
                .durationMonths(months).startDate(start).label("Prêt").build();
    }

    @Test
    void sommeDesCapitauxRembourses_egaleLePrincipal_etCrdFinalNul() {
        Loan l = loan("150000", "2.000", 240, LocalDate.of(2024, 1, 1));

        Map<Integer, LoanScheduleService.LoanYear> plan = service.yearlyBreakdown(l);
        BigDecimal totalCapital = plan.values().stream()
                .map(LoanScheduleService.LoanYear::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalCapital).isEqualByComparingTo("150000");
        assertThat(service.crdEnd(l, 2043)).isEqualByComparingTo("0"); // dernière année
    }

    @Test
    void interetsDecroissants_dAnneeEnAnnee() {
        Loan l = loan("100000", "3.000", 120, LocalDate.of(2024, 1, 1));

        BigDecimal prev = null;
        for (int y = 2024; y <= 2033; y++) {
            BigDecimal interest = service.forYear(l, y).interest();
            if (prev != null) assertThat(interest).isLessThan(prev);
            prev = interest;
        }
    }

    @Test
    void avantDeblocage_aucunFlux_niDette() {
        Loan l = loan("80000", "2.500", 180, LocalDate.of(2026, 7, 1));

        LoanScheduleService.LoanYear avant = service.forYear(l, 2025);
        assertThat(avant.interest()).isEqualByComparingTo("0");
        assertThat(avant.capital()).isEqualByComparingTo("0");
        assertThat(avant.insurance()).isEqualByComparingTo("0");
        assertThat(avant.crdEnd()).isEqualByComparingTo("0"); // pas encore débloqué
    }

    @Test
    void tauxZero_repartitLineairement() {
        Loan l = loan("12000", "0.000", 12, LocalDate.of(2024, 1, 1));

        LoanScheduleService.LoanYear y = service.forYear(l, 2024);
        assertThat(y.capital()).isEqualByComparingTo("12000");
        assertThat(y.interest()).isEqualByComparingTo("0");
        assertThat(y.crdEnd()).isEqualByComparingTo("0");
    }

    @Test
    void echeancier_trieParAnnee_etCouvreTouteLaDuree() {
        Loan l = loan("60000", "2.000", 60, LocalDate.of(2024, 6, 1));

        var schedule = service.schedule(l);

        assertThat(schedule).hasSize(6); // juin 2024 → mai 2029
        assertThat(schedule.get(0).year()).isEqualTo(2024);
        assertThat(schedule.get(5).year()).isEqualTo(2029);
        assertThat(schedule.get(5).crdEnd()).isEqualByComparingTo("0");
    }
}
