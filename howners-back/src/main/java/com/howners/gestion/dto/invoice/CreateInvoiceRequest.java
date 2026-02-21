package com.howners.gestion.dto.invoice;

import com.howners.gestion.domain.invoice.InvoiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID rentalId,
        @NotNull InvoiceType invoiceType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate issueDate,
        LocalDate dueDate
) {}
