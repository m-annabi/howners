package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.ContractStatus;
import jakarta.validation.constraints.Size;

public record UpdateContractRequest(
        @Size(max = 51200, message = "Custom content must not exceed 51200 characters")
        String customContent,  // Pour modifier le contenu avant signature

        ContractStatus status  // Pour changer le statut (DRAFT -> SENT par exemple)
) {
}
