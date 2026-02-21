package com.howners.gestion.dto.expense;

import com.howners.gestion.domain.expense.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(
        UUID propertyId,
        UUID rentalId,
        @NotNull ExpenseCategory category,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate expenseDate
) {}
