package com.howners.gestion.dto.audit;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.audit.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String userName,
        String entityType,
        UUID entityId,
        AuditAction action,
        String changes,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUser() != null ? auditLog.getUser().getId() : null,
                auditLog.getUser() != null ? auditLog.getUser().getFullName() : "Système",
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getChanges(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}
