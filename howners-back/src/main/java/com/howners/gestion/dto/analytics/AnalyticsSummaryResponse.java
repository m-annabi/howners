package com.howners.gestion.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummaryResponse(
        BigDecimal totalRevenue,
        double occupancyRate,
        long totalProperties,
        long activeRentals,
        List<MonthlyRevenue> monthlyRevenue,
        List<String> vacantProperties
) {
    public record MonthlyRevenue(
            String month,
            BigDecimal amount
    ) {}
}
