package com.howners.gestion.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RetenueDepotRequest(
        @NotNull List<Retenue> retenues
) {
    public record Retenue(
            @NotBlank String piece,
            String etatEntree,
            String etatSortie,
            @NotBlank String motif,
            @NotNull @DecimalMin(value = "0.0") BigDecimal montant
    ) {}
}
