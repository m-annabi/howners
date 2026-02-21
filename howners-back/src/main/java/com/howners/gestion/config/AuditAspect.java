package com.howners.gestion.config;

import com.howners.gestion.domain.audit.Audited;
import com.howners.gestion.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void auditMethod(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            String entityType = audited.entityType();
            if (entityType.isEmpty()) {
                entityType = joinPoint.getTarget().getClass().getSimpleName().replace("Service", "");
            }

            UUID entityId = extractEntityId(result, joinPoint);
            if (entityId != null) {
                auditService.logAction(audited.action(), entityType, entityId);
            }
        } catch (Exception e) {
            log.warn("Failed to audit method {}: {}", joinPoint.getSignature().getName(), e.getMessage());
        }
    }

    private UUID extractEntityId(Object result, JoinPoint joinPoint) {
        // Try to extract ID from the result object
        if (result == null) return null;

        try {
            var method = result.getClass().getMethod("id");
            Object id = method.invoke(result);
            if (id instanceof UUID uuid) return uuid;
            if (id instanceof String str) return UUID.fromString(str);
        } catch (NoSuchMethodException e) {
            // Try getId()
            try {
                var method = result.getClass().getMethod("getId");
                Object id = method.invoke(result);
                if (id instanceof UUID uuid) return uuid;
            } catch (Exception ex) {
                // Fallback: try arguments
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback: try first UUID argument
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID uuid) return uuid;
        }
        return null;
    }
}
