package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.ContractResponse;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.dto.request.UpdateProfileRequest;
import com.howners.gestion.service.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class TenantController {

    private final TenantService tenantService;

    /**
     * GET /api/tenants/me - Récupérer les informations du locataire connecté
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<UserResponse> getMyProfile() {
        log.info("Fetching tenant profile");
        return ResponseEntity.ok(tenantService.getMyProfile());
    }

    /**
     * PUT /api/tenants/me - Mettre à jour le profil du locataire
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        log.info("Updating tenant profile");
        return ResponseEntity.ok(tenantService.updateMyProfile(request));
    }

    /**
     * GET /api/tenants/me/rentals - Récupérer les locations du locataire
     */
    @GetMapping("/me/rentals")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<RentalResponse>> getMyRentals() {
        log.info("Fetching tenant rentals");
        return ResponseEntity.ok(tenantService.getMyRentals());
    }

    /**
     * GET /api/tenants/me/contracts - Récupérer les contrats du locataire
     */
    @GetMapping("/me/contracts")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<ContractResponse>> getMyContracts() {
        log.info("Fetching tenant contracts");
        return ResponseEntity.ok(tenantService.getMyContracts());
    }

    /**
     * GET /api/tenants/me/documents - Récupérer les documents du locataire
     */
    @GetMapping("/me/documents")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {
        log.info("Fetching tenant documents");
        return ResponseEntity.ok(tenantService.getMyDocuments());
    }
}
