package com.howners.gestion.dto.response;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalProperties,
        long activeRentals,
        long pendingRentals,
        long terminatedRentals,
        BigDecimal monthlyRevenue,
        String currency,
        RecentActivity recentActivity,
        TenantInfo tenantInfo
) {
    public record RecentActivity(
            PropertyResponse latestProperty,
            RentalResponse latestRental
    ) {}

    public record TenantInfo(
            long totalApplications,
            long pendingApplications,
            long pendingInvitations,
            long totalInvitations,
            boolean searchProfileActive,
            long unreadMessages
    ) {}

    // Factory for owner/admin (no tenant info)
    public static DashboardStatsResponse forOwner(
            long totalProperties, long activeRentals, long pendingRentals,
            long terminatedRentals, BigDecimal monthlyRevenue, String currency,
            RecentActivity recentActivity) {
        return new DashboardStatsResponse(totalProperties, activeRentals, pendingRentals,
                terminatedRentals, monthlyRevenue, currency, recentActivity, null);
    }

    // Factory for tenant
    public static DashboardStatsResponse forTenant(
            long activeRentals, long pendingRentals, long terminatedRentals,
            BigDecimal monthlyRevenue, String currency, RecentActivity recentActivity,
            TenantInfo tenantInfo) {
        return new DashboardStatsResponse(0, activeRentals, pendingRentals,
                terminatedRentals, monthlyRevenue, currency, recentActivity, tenantInfo);
    }
}
