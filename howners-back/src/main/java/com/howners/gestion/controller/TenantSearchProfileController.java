package com.howners.gestion.controller;

import com.howners.gestion.dto.search.CreateTenantSearchProfileRequest;
import com.howners.gestion.dto.search.TenantSearchProfileResponse;
import com.howners.gestion.service.search.TenantSearchProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant-search-profile")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class TenantSearchProfileController {

    private final TenantSearchProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<TenantSearchProfileResponse> getMyProfile() {
        log.info("Fetching my search profile");
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<TenantSearchProfileResponse> createOrUpdate(@Valid @RequestBody CreateTenantSearchProfileRequest request) {
        log.info("Creating/updating search profile");
        return ResponseEntity.ok(profileService.createOrUpdate(request));
    }

    @PutMapping("/me/activate")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<TenantSearchProfileResponse> activate() {
        log.info("Activating search profile");
        return ResponseEntity.ok(profileService.activate());
    }

    @PutMapping("/me/deactivate")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<TenantSearchProfileResponse> deactivate() {
        log.info("Deactivating search profile");
        return ResponseEntity.ok(profileService.deactivate());
    }
}
