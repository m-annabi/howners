package com.howners.gestion.controller;

import com.howners.gestion.dto.rating.TenantScoreResponse;
import com.howners.gestion.service.rating.TenantScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant-scoring")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class TenantScoringController {

    private final TenantScoringService scoringService;

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantScoreResponse> getScore(@PathVariable UUID tenantId) {
        log.info("Calculating score for tenant: {}", tenantId);
        TenantScoreResponse score = scoringService.calculateScore(tenantId);
        return ResponseEntity.ok(score);
    }

    @GetMapping("/compare")
    public ResponseEntity<List<TenantScoreResponse>> compareScores(@RequestParam List<UUID> tenantIds) {
        log.info("Comparing scores for {} tenants", tenantIds.size());
        List<TenantScoreResponse> scores = scoringService.compareScores(tenantIds);
        return ResponseEntity.ok(scores);
    }
}
