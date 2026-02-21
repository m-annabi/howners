package com.howners.gestion.dto.response;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.rental.RentalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID tenantId,
        String tenantName,
        String tenantEmail,
        RentalType rentalType,
        RentalStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        String currency,
        BigDecimal depositAmount,
        BigDecimal charges,
        Integer paymentDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RentalResponse from(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                rental.getProperty().getId(),
                rental.getProperty().getName(),
                rental.getTenant() != null ? rental.getTenant().getId() : null,
                rental.getTenant() != null ? rental.getTenant().getFullName() : null,
                rental.getTenant() != null ? rental.getTenant().getEmail() : null,
                rental.getRentalType(),
                rental.getStatus(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getMonthlyRent(),
                rental.getCurrency(),
                rental.getDepositAmount(),
                rental.getCharges(),
                rental.getPaymentDay(),
                rental.getCreatedAt(),
                rental.getUpdatedAt()
        );
    }
}
