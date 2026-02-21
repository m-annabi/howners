package com.howners.gestion.dto.inventory;

import com.howners.gestion.domain.inventory.EtatDesLieuxType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEtatDesLieuxRequest(
        @NotNull EtatDesLieuxType type,
        @NotNull LocalDate inspectionDate,
        String roomConditions,
        String meterReadings,
        Integer keysCount,
        String keysDescription,
        String generalComments
) {
}
