package com.howners.gestion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is required")
        String email
) {}
