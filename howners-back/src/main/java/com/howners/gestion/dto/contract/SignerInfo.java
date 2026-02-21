package com.howners.gestion.dto.contract;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignerInfo(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotNull Integer order
) {
}
