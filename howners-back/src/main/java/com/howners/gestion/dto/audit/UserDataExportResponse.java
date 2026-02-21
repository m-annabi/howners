package com.howners.gestion.dto.audit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record UserDataExportResponse(
        String exportDate,
        UserInfo personalInfo,
        List<Map<String, Object>> properties,
        List<Map<String, Object>> rentals,
        List<Map<String, Object>> contracts,
        List<Map<String, Object>> payments,
        List<Map<String, Object>> documents,
        List<ConsentResponse> consents
) {
    public record UserInfo(
            String email,
            String firstName,
            String lastName,
            String phone,
            String role,
            LocalDateTime createdAt
    ) {
    }
}
