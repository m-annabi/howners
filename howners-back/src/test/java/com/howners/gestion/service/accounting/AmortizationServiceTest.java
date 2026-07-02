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
    void plan_sumsToBase() {
        AmortizableAsset a = asset("9000", 3, LocalDate.of(2024, 3, 15));
        List<AmortizationService.AmortizationLine> plan = service.plan(a);
        BigDecimal sum = plan.stream().map(AmortizationService.AmortizationLine::annuite)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("9000.00");
        assertThat(plan.get(plan.size() - 1).vnc()).isEqualByComparingTo("0");
    }
}
