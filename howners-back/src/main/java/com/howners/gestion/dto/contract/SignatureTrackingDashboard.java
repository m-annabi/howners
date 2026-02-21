package com.howners.gestion.dto.contract;

import java.util.List;

public record SignatureTrackingDashboard(
        long totalRequests,
        long pendingRequests,
        long sentRequests,
        long viewedRequests,
        long signedRequests,
        long declinedRequests,
        long expiredRequests,
        List<SignatureRequestResponse> recentRequests
) {
}
