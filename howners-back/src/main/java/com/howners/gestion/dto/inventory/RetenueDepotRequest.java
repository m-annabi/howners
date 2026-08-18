package com.howners.gestion.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RetenueDepotRequest(
        // @Valid cascade la validation sur chaque Retenue (sinon @DecimalMin ci-dessous
        // est ignoré et des montants négatifs passent → total de retenues négatif).
        @NotNull @Valid List<Retenue> retenues
) {
    public record Retenue(
            @NotBlank String piece,
            String etatEntree,
            String etatSortie,
            @NotBlank String motif,
            @NotNull @DecimalMin(value = "0.0") BigDecimal montant
    ) {}
}
