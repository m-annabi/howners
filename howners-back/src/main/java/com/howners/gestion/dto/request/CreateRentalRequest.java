package com.howners.gestion.dto.request;

import com.howners.gestion.domain.rental.RentalType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRentalRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        UUID tenantId,  // Peut être null si on crée le locataire en même temps

        // Informations du locataire (si tenantId est null)
        @Email
        String tenantEmail,
        String tenantFirstName,
        String tenantLastName,
        String tenantPhone,

        @NotNull(message = "Rental type is required")
        RentalType rentalType,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be in the present or future")
        LocalDate startDate,

        LocalDate endDate,  // Peut être null pour location longue durée

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
    }
}
