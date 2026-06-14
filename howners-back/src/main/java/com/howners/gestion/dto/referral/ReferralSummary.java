package com.howners.gestion.dto.referral;

import java.time.LocalDateTime;
import java.util.List;

public record ReferralSummary(
        String code,
        String shareUrl,
        long pendingCount,
        long convertedCount,
        List<RefereeItem> referees
) {
    public record RefereeItem(
            String name,
            String status,
            LocalDateTime createdAt,
            LocalDateTime rewardedAt
    ) {}
}
