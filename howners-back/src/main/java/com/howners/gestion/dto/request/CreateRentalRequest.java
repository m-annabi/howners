package com.howners.gestion.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRentalRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Monthly rent must be positive")
        BigDecimal monthlyRent,

        String currency,

        @DecimalMin(value = "0.0", message = "Deposit amount must be positive")
        BigDecimal depositAmount,

        @DecimalMin(value = "0.0", message = "Charges must be positive")
        BigDecimal charges,

        @Min(value = 1, message = "Payment day must be between 1 and 31")
        @Max(value = 31, message = "Payment day must be between 1 and 31")
        Integer paymentDay
) {
    public CreateRentalRequest {
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
