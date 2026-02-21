package com.howners.gestion.dto.invoice;

import com.howners.gestion.domain.invoice.Invoice;
import com.howners.gestion.domain.invoice.InvoiceStatus;
import com.howners.gestion.domain.invoice.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        String tenantName,
        String invoiceNumber,
        InvoiceType invoiceType,
        BigDecimal amount,
        String currency,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        UUID paymentId,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(
                i.getId(),
                i.getRental().getId(),
                i.getRental().getProperty().getName(),
                i.getRental().getTenant() != null ? i.getRental().getTenant().getFullName() : "",
                i.getInvoiceNumber(),
                i.getInvoiceType(),
                i.getAmount(),
                i.getCurrency(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getStatus(),
                i.getPayment() != null ? i.getPayment().getId() : null,
                i.getDocument() != null ? i.getDocument().getId() : null,
                i.getCreatedAt()
        );
    }
}
