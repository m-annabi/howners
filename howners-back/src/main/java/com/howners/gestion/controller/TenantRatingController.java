package com.howners.gestion.controller;

import com.howners.gestion.dto.rating.CreateTenantRatingRequest;
import com.howners.gestion.dto.rating.TenantRatingResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.service.rating.TenantRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant-ratings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class TenantRatingController {

    private final TenantRatingService ratingService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TenantRatingResponse> create(@Valid @RequestBody CreateTenantRatingRequest request) {
        return ResponseEntity.ok(ratingService.create(request));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TenantRatingResponse>> getRatingsForTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ratingService.getRatingsForTenant(tenantId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<TenantRatingResponse>> getMyRatings() {
        return ResponseEntity.ok(ratingService.getMyRatings());
    }

    @GetMapping("/tenant/{tenantId}/profile")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<UserResponse> getTenantProfile(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ratingService.getTenantProfile(tenantId));
    }
}
