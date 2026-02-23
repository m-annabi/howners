package com.howners.gestion.dto.contract;

import jakarta.validation.constraints.NotBlank;

public record SignContractRequest(
        @NotBlank String signatureData,
        @NotBlank String signerName
) {}
