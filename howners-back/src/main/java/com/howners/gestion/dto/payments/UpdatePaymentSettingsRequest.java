package com.howners.gestion.dto.payments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePaymentSettingsRequest(
        @Size(max = 2000, message = "Les coordonnées de paiement ne doivent pas dépasser 2000 caractères")
        String paymentInstructions,

        @NotNull(message = "acceptOnlinePayments est requis")
        Boolean acceptOnlinePayments
) {}
