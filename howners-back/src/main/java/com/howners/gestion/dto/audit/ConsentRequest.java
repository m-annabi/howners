package com.howners.gestion.dto.audit;

import com.howners.gestion.domain.audit.ConsentType;
import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
        @NotNull ConsentType consentType,
        @NotNull Boolean granted
) {
}
