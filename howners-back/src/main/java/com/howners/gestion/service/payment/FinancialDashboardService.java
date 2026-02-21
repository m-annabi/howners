package com.howners.gestion.service.payment;

import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.dto.response.FinancialDashboardResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialDashboardService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getFinancialDashboard() {
        UUID currentUserId = AuthService.getCurrentUserId();
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        LocalDate now = LocalDate.now();
        LocalDate twelveMonthsAgo = now.minusMonths(12).withDayOfMonth(1);

        // Total revenue (paid payments over last 12 months)
        BigDecimal totalRevenue = paymentRepository.sumPaidAmountByOwnerAndPeriod(
                currentUserId,
                twelveMonthsAgo.atStartOfDay(),
                now.plusDays(1).atStartOfDay()
        );

        // Total expenses over last 12 months
        BigDecimal totalExpenses = expenseRepository.sumExpensesByOwnerAndPeriod(
                currentUserId,
                twelveMonthsAgo,
                now.plusDays(1)
        );

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        // Pending payments
        long pendingCount = paymentRepository.findByOwnerId(currentUserId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .count();
        BigDecimal pendingAmount = paymentRepository.findByOwnerId(currentUserId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Overdue payments
        long overdueCount = paymentRepository.findByOwnerId(currentUserId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.LATE)
                .count();

        // Monthly breakdown for last 12 months
        List<FinancialDashboardResponse.MonthlyBreakdown> monthlyBreakdown = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay();
            LocalDate monthStartDate = ym.atDay(1);
            LocalDate monthEndDate = ym.plusMonths(1).atDay(1);

            BigDecimal monthRevenue = paymentRepository.sumPaidAmountByOwnerAndPeriod(
                    currentUserId, monthStart, monthEnd);
            BigDecimal monthExpenses = expenseRepository.sumExpensesByOwnerAndPeriod(
                    currentUserId, monthStartDate, monthEndDate);

            monthlyBreakdown.add(new FinancialDashboardResponse.MonthlyBreakdown(
                    ym.format(monthFormatter),
                    monthRevenue,
                    monthExpenses,
                    monthRevenue.subtract(monthExpenses)
            ));
        }

        // Expenses by category (all time for the owner)
        // Note: We aggregate across all properties
        List<FinancialDashboardResponse.CategoryBreakdown> expensesByCategory =
                expenseRepository.findByOwnerId(currentUserId).stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCategory().name(),
                                Collectors.reducing(BigDecimal.ZERO, e -> e.getAmount(), BigDecimal::add)
                        ))
                        .entrySet().stream()
                        .map(entry -> new FinancialDashboardResponse.CategoryBreakdown(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

        return new FinancialDashboardResponse(
                totalRevenue,
                totalExpenses,
                netIncome,
                pendingAmount,
                overdueCount,
                monthlyBreakdown,
                expensesByCategory
        );
    }
}
