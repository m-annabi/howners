package com.howners.gestion.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FinancialDashboardResponse(
        BigDecimal totalRevenue,
        BigDecimal totalExpenses,
        BigDecimal netIncome,
        BigDecimal pendingPayments,
        long overduePayments,
        List<MonthlyBreakdown> monthlyBreakdown,
        List<CategoryBreakdown> expensesByCategory
) {
    public record MonthlyBreakdown(
            String month,
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal net
    ) {}

    public record CategoryBreakdown(
            String category,
            BigDecimal amount
    ) {}
}
