package com.howners.gestion.service.export;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateFinancialCsv(int year) {
        UUID ownerId = getCurrentUserId();

        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        LocalDate yearStartDate = LocalDate.of(year, 1, 1);
        LocalDate yearEndDate = LocalDate.of(year + 1, 1, 1);

        // Paiements reçus pour l'année
        List<Payment> payments = paymentRepository.findByOwnerId(ownerId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getPaidAt() != null
                        && !p.getPaidAt().isBefore(yearStart)
                        && p.getPaidAt().isBefore(yearEnd))
                .toList();

        // Dépenses pour l'année
        List<Expense> expenses = expenseRepository.findByOwnerId(ownerId).stream()
                .filter(e -> !e.getExpenseDate().isBefore(yearStartDate)
                        && e.getExpenseDate().isBefore(yearEndDate))
                .toList();

        StringBuilder csv = new StringBuilder();
        // En-tête BOM UTF-8 pour Excel
        csv.append('﻿');
        csv.append("Date;Type;Bien;Montant;Description\n");

        // Lignes de revenus
        for (Payment p : payments) {
            String date = p.getPaidAt().format(DATE_FMT);
            String propertyName = escapeCsv(p.getRental().getProperty().getName());
            String amount = p.getAmount().toPlainString();
            String description = escapeCsv(p.getPaymentType().name());
            csv.append(date).append(";Revenu;").append(propertyName).append(";")
               .append(amount).append(";").append(description).append("\n");
        }

        // Lignes de dépenses
        for (Expense e : expenses) {
            String date = e.getExpenseDate().format(DATE_FMT);
            String propertyName = e.getProperty() != null
                    ? escapeCsv(e.getProperty().getName())
                    : "";
            BigDecimal amount = e.getAmount().negate();
            String description = escapeCsv(
                    e.getDescription() != null ? e.getDescription() : e.getCategory().name()
            );
            csv.append(date).append(";Depense;").append(propertyName).append(";")
               .append(amount.toPlainString()).append(";").append(description).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Échapper les guillemets et entourer si nécessaire
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private UUID getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
