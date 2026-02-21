package com.howners.gestion.dto.response;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalProperties,
        long activeRentals,
        long pendingRentals,
        long terminatedRentals,
        BigDecimal monthlyRevenue,
        String currency,
        RecentActivity recentActivity
) {
    public record RecentActivity(
            PropertyResponse latestProperty,
            RentalResponse latestRental
    ) {}
}
