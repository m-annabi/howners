package com.howners.gestion.dto.document;

import jakarta.validation.constraints.NotNull;

public record SetLegalHoldRequest(
        @NotNull Boolean hold
) {}
