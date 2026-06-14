package com.howners.gestion.dto.user;

import com.howners.gestion.dto.response.FinancialDashboardResponse;

import java.util.UUID;

public record ApercuCompteResponse(
        UUID agenceUserId,
        String agenceNom,
        String agenceEmail,
        long totalProperties,
        long activeRentals,
        FinancialDashboardResponse finances
) {
}
