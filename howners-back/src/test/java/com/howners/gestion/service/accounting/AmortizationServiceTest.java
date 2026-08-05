package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AmortizationServiceTest {

    private final AmortizationService service = new AmortizationService();

    private AmortizableAsset asset(String base, int duration, LocalDate start) {
        return AmortizableAsset.builder()
                .type(AssetType.MOBILIER).label("Test").base(new BigDecimal(base))
                .durationYears(duration).startDate(start).build();
    }

    @Test
    void linearFullYears_cumulEqualsBase() {
        AmortizableAsset a = asset("10000", 5, LocalDate.of(2024, 1, 1));

        assertThat(service.annuite(a, 2024)).isEqualByComparingTo("2000.00");
        assertThat(service.annuite(a, 2026)).isEqualByComparingTo("2000.00");
        assertThat(service.cumul(a, 2028)).isEqualByComparingTo("10000.00"); // 5e année
        assertThat(service.vnc(a, 2028)).isEqualByComparingTo("0");
        assertThat(service.annuite(a, 2029)).isEqualByComparingTo("0"); // au-delà de la durée
    }

    @Test
    void prorataFirstYear_isPartialAndCapped() {
        AmortizableAsset a = asset("12000", 10, LocalDate.of(2024, 7, 1)); // annuité pleine = 1200
        BigDecimal first = service.annuite(a, 2024);
        assertThat(first).isLessThan(new BigDecimal("1200.00"));
        assertThat(first).isGreaterThan(BigDecimal.ZERO);
        assertThat(service.annuite(a, 2025)).isEqualByComparingTo("1200.00");
    }

    @Test
    void prorata30_360_premiereAnnee_exacte() {
        // Mise en service 01/07 → 6 mois × 30 / 360 = 0,5 → 600,00 exactement (base 30/360, pas actual/360).
        AmortizableAsset a = asset("12000", 10, LocalDate.of(2024, 7, 1)); // annuité pleine = 1200
        assertThat(service.annuite(a, 2024)).isEqualByComparingTo("600.00");
        assertThat(service.annuite(a, 2025)).isEqualByComparingTo("1200.00");
    }

    @Test
    void anneeComplementaire_absorbeLeProrata_sansAnnuiteBallon() {
        // Le plan court sur durée + 1 années civiles ; aucune annuité ne dépasse l'annuité pleine.
        AmortizableAsset a = asset("12000", 10, LocalDate.of(2024, 7, 1)); // pleine = 1200
        // Dernière année pleine (2033) : 1200, PAS gonflée.
        assertThat(service.annuite(a, 2033)).isEqualByComparingTo("1200.00");
        // Année complémentaire (2034 = 2024 + 10) : le solde résiduel = 600, ≤ annuité pleine.
        assertThat(service.annuite(a, 2034)).isEqualByComparingTo("600.00");
        assertThat(service.annuite(a, 2035)).isEqualByComparingTo("0");
        // Aucune annuité ne dépasse l'annuité linéaire pleine.
        for (AmortizationService.AmortizationLine l : service.plan(a)) {
            assertThat(l.annuite()).isLessThanOrEqualTo(new BigDecimal("1200.00"));
        }
        assertThat(service.cumul(a, 2034)).isEqualByComparingTo("12000.00");
    }

    @Test
    void miseEnServiceAu1erJanvier_pasDAnneeComplementaire() {
        AmortizableAsset a = asset("10000", 5, LocalDate.of(2024, 1, 1));
        assertThat(service.plan(a)).hasSize(5); // pas d'année complémentaire (prorata = 1)
        assertThat(service.annuite(a, 2029)).isEqualByComparingTo("0");
    }

    @Test
    void plan_sumsToBase() {
        AmortizableAsset a = asset("9000", 3, LocalDate.of(2024, 3, 15));
        List<AmortizationService.AmortizationLine> plan = service.plan(a);
        BigDecimal sum = plan.stream().map(AmortizationService.AmortizationLine::annuite)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("9000.00");
        assertThat(plan.get(plan.size() - 1).vnc()).isEqualByComparingTo("0");
    }
}
