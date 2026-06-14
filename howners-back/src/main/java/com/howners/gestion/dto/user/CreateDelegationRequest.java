package com.howners.gestion.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateDelegationRequest(
        @NotBlank @Email String email
) {
}
