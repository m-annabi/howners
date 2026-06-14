package com.howners.gestion.dto.rental;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateIrlIndiceRequest(
        @NotNull @Min(2000) @Max(2100) Integer annee,
        @NotNull @Min(1) @Max(4) Integer trimestre,
        @NotNull BigDecimal valeur
) {
}
