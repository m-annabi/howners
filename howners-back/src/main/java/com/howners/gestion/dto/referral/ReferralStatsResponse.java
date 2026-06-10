package com.howners.gestion.dto.referral;

public record ReferralStatsResponse(
        long total,
        long successful,
        long pending
) {}
