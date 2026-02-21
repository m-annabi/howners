package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.ContractStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vue publique d'un contrat (accessible via token)
 * Ne contient que les informations nécessaires pour la signature
 */
@Builder
public record ContractPublicView(
        UUID contractId,
        String contractNumber,
        ContractStatus status,
        String propertyName,
        String propertyAddress,
        String ownerName,
        String tenantName,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        String monthlyRent,
        LocalDateTime createdAt,
        String documentUrl
) {}
