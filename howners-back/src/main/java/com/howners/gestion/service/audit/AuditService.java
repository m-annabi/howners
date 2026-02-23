package com.howners.gestion.service.audit;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.audit.AuditLog;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.audit.AuditLogResponse;
import com.howners.gestion.repository.AuditLogRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void logAction(AuditAction action, String entityType, UUID entityId, String changes) {
        try {
            UUID userId = null;
            try {
                userId = AuthService.getCurrentUserId();
            } catch (Exception e) {
                // System action, no user context
            }

            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .changes(changes)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} {} on {} {}", action, entityType, entityId, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    public void logAction(AuditAction action, String entityType, UUID entityId) {
        logAction(action, entityType, entityId, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findWithFilters(String entityType, AuditAction action, UUID userId, Pageable pageable) {
        return auditLogRepository.findWithFilters(entityType, action, userId, pageable)
                .map(AuditLogResponse::from);
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // No request context
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            // No request context
        }
        return "unknown";
    }
}
