package com.howners.gestion.dto.request;

import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.rental.RentalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateRentalRequest(
        RentalType rentalType,

        RentalStatus status,

        LocalDate startDate,

        LocalDate endDate,

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
) {}
