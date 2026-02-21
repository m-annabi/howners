package com.howners.gestion.repository;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<AuditLog> findByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:userId IS NULL OR a.user.id = :userId) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findWithFilters(
            @Param("entityType") String entityType,
            @Param("action") AuditAction action,
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
