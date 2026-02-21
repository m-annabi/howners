package com.howners.gestion.dto.inventory;

import com.howners.gestion.domain.inventory.EtatDesLieux;
import com.howners.gestion.domain.inventory.EtatDesLieuxType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EtatDesLieuxResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        String tenantName,
        EtatDesLieuxType type,
        LocalDate inspectionDate,
        String roomConditions,
        String meterReadings,
        Integer keysCount,
        String keysDescription,
        String generalComments,
        Boolean ownerSigned,
        Boolean tenantSigned,
        LocalDateTime ownerSignedAt,
        LocalDateTime tenantSignedAt,
        String createdByName,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static EtatDesLieuxResponse from(EtatDesLieux e) {
        return new EtatDesLieuxResponse(
                e.getId(),
                e.getRental().getId(),
                e.getRental().getProperty().getName(),
                e.getRental().getTenant() != null ? e.getRental().getTenant().getFullName() : null,
                e.getType(),
                e.getInspectionDate(),
                e.getRoomConditions(),
                e.getMeterReadings(),
                e.getKeysCount(),
                e.getKeysDescription(),
                e.getGeneralComments(),
                e.getOwnerSigned(),
                e.getTenantSigned(),
                e.getOwnerSignedAt(),
                e.getTenantSignedAt(),
                e.getCreatedBy().getFullName(),
                e.getDocument() != null ? e.getDocument().getId() : null,
                e.getCreatedAt()
        );
    }
}
