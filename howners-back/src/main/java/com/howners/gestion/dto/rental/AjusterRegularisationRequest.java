package com.howners.gestion.dto.rental;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Correction manuelle du montant des charges réelles d'une régularisation, avec motif et justificatif. */
public record AjusterRegularisationRequest(
        @NotNull @DecimalMin("0.00") BigDecimal chargesReelles,
        @NotBlank @Size(min = 10, max = 1000, message = "Le motif doit faire entre 10 et 1000 caractères") String motif,
        @NotNull(message = "Un justificatif est obligatoire") UUID justificatifDocumentId
) {}
