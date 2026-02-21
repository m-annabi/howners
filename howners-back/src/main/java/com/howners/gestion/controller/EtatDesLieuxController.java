package com.howners.gestion.controller;

import com.howners.gestion.dto.inventory.CreateEtatDesLieuxRequest;
import com.howners.gestion.dto.inventory.EtatDesLieuxResponse;
import com.howners.gestion.service.inventory.EtatDesLieuxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class EtatDesLieuxController {

    private final EtatDesLieuxService edlService;

    @GetMapping("/api/etat-des-lieux")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EtatDesLieuxResponse>> getMyEdls() {
        log.info("Fetching états des lieux for current user");
        List<EtatDesLieuxResponse> edls = edlService.findByCurrentUser();
        return ResponseEntity.ok(edls);
    }

    @GetMapping("/api/rentals/{rentalId}/etat-des-lieux")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<EtatDesLieuxResponse>> getByRental(@PathVariable UUID rentalId) {
        log.info("Fetching états des lieux for rental: {}", rentalId);
        List<EtatDesLieuxResponse> edls = edlService.findByRentalId(rentalId);
        return ResponseEntity.ok(edls);
    }

    @PostMapping("/api/rentals/{rentalId}/etat-des-lieux")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<EtatDesLieuxResponse> create(
            @PathVariable UUID rentalId,
            @Valid @RequestBody CreateEtatDesLieuxRequest request) {
        try {
            log.info("Creating état des lieux for rental: {}", rentalId);
            EtatDesLieuxResponse edl = edlService.create(rentalId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(edl);
        } catch (IOException e) {
            log.error("Error creating état des lieux: {}", e.getMessage());
            throw new RuntimeException("Failed to generate état des lieux PDF", e);
        }
    }

    @GetMapping("/api/rentals/{rentalId}/etat-des-lieux/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<EtatDesLieuxResponse> getById(
            @PathVariable UUID rentalId,
            @PathVariable UUID id) {
        log.info("Fetching état des lieux: {}", id);
        EtatDesLieuxResponse edl = edlService.findById(id);
        return ResponseEntity.ok(edl);
    }

    @PutMapping("/api/rentals/{rentalId}/etat-des-lieux/{id}/sign")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<EtatDesLieuxResponse> sign(
            @PathVariable UUID rentalId,
            @PathVariable UUID id,
            @RequestParam String role) {
        log.info("Signing état des lieux {} as {}", id, role);
        EtatDesLieuxResponse edl = edlService.sign(id, role);
        return ResponseEntity.ok(edl);
    }

    @GetMapping("/api/rentals/{rentalId}/etat-des-lieux/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID rentalId,
            @PathVariable UUID id) throws IOException {
        log.info("Downloading PDF for état des lieux: {}", id);
        byte[] pdfBytes = edlService.downloadPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=etat-des-lieux-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
