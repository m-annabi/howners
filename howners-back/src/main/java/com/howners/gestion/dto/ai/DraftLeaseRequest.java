package com.howners.gestion.dto.ai;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DraftLeaseRequest(
        @NotNull UUID rentalId,
        Boolean furnished,
        Integer leaseMonths,
        Integer noticeMonths,
        Boolean petsAllowed,
        String customClauses
) {}
