package com.howners.gestion.dto.expense;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID rentalId,
        ExpenseCategory category,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getProperty() != null ? e.getProperty().getId() : null,
                e.getProperty() != null ? e.getProperty().getName() : null,
                e.getRental() != null ? e.getRental().getId() : null,
                e.getCategory(),
                e.getDescription(),
                e.getAmount(),
                e.getCurrency(),
                e.getExpenseDate(),
                e.getDocument() != null ? e.getDocument().getId() : null,
                e.getCreatedAt()
        );
    }
}
