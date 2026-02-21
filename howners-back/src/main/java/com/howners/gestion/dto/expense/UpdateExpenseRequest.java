package com.howners.gestion.dto.expense;

import com.howners.gestion.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateExpenseRequest(
        UUID propertyId,
        UUID rentalId,
        ExpenseCategory category,
        String description,
        BigDecimal amount,
        LocalDate expenseDate
) {}
