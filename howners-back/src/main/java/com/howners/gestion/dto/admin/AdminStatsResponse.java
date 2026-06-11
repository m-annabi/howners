package com.howners.gestion.dto.admin;

import java.math.BigDecimal;
import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        long totalOwners,
        long totalTenants,
        long totalProperties,
        long totalRentals,
        long totalContracts,
        long newUsersThisMonth,
        Map<String, Long> activeSubscriptionsByPlan,
        BigDecimal mrr
) {}
