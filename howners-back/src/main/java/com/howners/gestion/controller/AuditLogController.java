package com.howners.gestion.controller;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.dto.audit.AuditLogResponse;
import com.howners.gestion.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AuditLogResponse> logs = auditService.findWithFilters(entityType, action, userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogResponse>> getByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId
    ) {
        return ResponseEntity.ok(auditService.findByEntity(entityType, entityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.findByUser(userId, pageable));
    }
}
