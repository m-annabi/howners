package com.howners.gestion.controller;

import com.howners.gestion.dto.rating.*;
import com.howners.gestion.service.rating.TenantRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class TenantRatingController {

    private final TenantRatingService tenantRatingService;

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<List<TenantRatingResponse>> getRatingsByTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantRatingService.findByTenantId(tenantId));
    }

    @GetMapping("/tenant/{tenantId}/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<TenantRatingSummaryResponse> getTenantRatingSummary(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantRatingService.getSummaryByTenantId(tenantId));
    }

    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<List<TenantRatingResponse>> getRatingsByRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(tenantRatingService.findByRentalId(rentalId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<List<TenantRatingResponse>> getMyRatings() {
        return ResponseEntity.ok(tenantRatingService.findMyRatings());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<TenantRatingResponse> getRating(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantRatingService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<TenantRatingResponse> createRating(@Valid @RequestBody CreateTenantRatingRequest request) {
        TenantRatingResponse rating = tenantRatingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<TenantRatingResponse> updateRating(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRatingRequest request
    ) {
        return ResponseEntity.ok(tenantRatingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONCIERGE', 'ADMIN')")
    public ResponseEntity<Void> deleteRating(@PathVariable UUID id) {
        tenantRatingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
