package com.howners.gestion.dto.referral;

import jakarta.validation.constraints.NotBlank;

public record ApplyReferralRequest(
        @NotBlank(message = "Le code de parrainage est requis")
        String code
) {}
