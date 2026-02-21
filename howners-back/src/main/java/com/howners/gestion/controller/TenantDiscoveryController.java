package com.howners.gestion.controller;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.dto.search.TenantSearchResultResponse;
import com.howners.gestion.service.search.TenantDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant-discovery")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class TenantDiscoveryController {

    private final TenantDiscoveryService discoveryService;

    @GetMapping
    public ResponseEntity<List<TenantSearchResultResponse>> searchTenants(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) String sortBy) {
        log.info("Searching tenants - city: {}, dept: {}, postalCode: {}, listingId: {}", city, department, postalCode, listingId);
        List<TenantSearchResultResponse> results = discoveryService.searchTenants(
                city, department, postalCode, budgetMin, budgetMax, propertyType, listingId, sortBy);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<TenantSearchResultResponse> getTenantProfile(
            @PathVariable UUID profileId,
            @RequestParam(required = false) UUID listingId) {
        log.info("Fetching tenant profile: {} with listing: {}", profileId, listingId);
        return ResponseEntity.ok(discoveryService.getTenantProfile(profileId, listingId));
    }
}
