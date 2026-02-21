package com.howners.gestion.dto.receipt;

import com.howners.gestion.domain.receipt.Receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        String tenantName,
        String ownerName,
        String receiptNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal amount,
        String currency,
        UUID paymentId,
        UUID documentId,
        String documentUrl,
        LocalDateTime createdAt
) {
    public static ReceiptResponse from(Receipt r) {
        return from(r, null);
    }

    public static ReceiptResponse from(Receipt r, String documentUrl) {
        return new ReceiptResponse(
                r.getId(),
                r.getRental().getId(),
                r.getRental().getProperty().getName(),
                r.getRental().getTenant() != null ? r.getRental().getTenant().getFullName() : "",
                r.getRental().getProperty().getOwner().getFullName(),
                r.getReceiptNumber(),
                r.getPeriodStart(),
                r.getPeriodEnd(),
                r.getAmount(),
                r.getCurrency(),
                r.getPayment().getId(),
                r.getDocument() != null ? r.getDocument().getId() : null,
                documentUrl,
                r.getCreatedAt()
        );
    }
}
