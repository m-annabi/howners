package com.howners.gestion.controller;

import com.howners.gestion.dto.rental.CreateIrlIndiceRequest;
import com.howners.gestion.dto.rental.IrlIndiceResponse;
import com.howners.gestion.dto.rental.RevisionLoyerResponse;
import com.howners.gestion.service.rental.RevisionLoyerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RentRevisionController {

    private final RevisionLoyerService revisionLoyerService;

    @GetMapping("/irl-indices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IrlIndiceResponse>> getIndices() {
        return ResponseEntity.ok(revisionLoyerService.getIndices());
    }

    @PostMapping("/irl-indices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IrlIndiceResponse> addIndice(@Valid @RequestBody CreateIrlIndiceRequest request) {
        return ResponseEntity.ok(revisionLoyerService.addIndice(request));
    }

    @GetMapping("/rentals/{rentalId}/revisions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RevisionLoyerResponse>> getRevisions(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(revisionLoyerService.findByRentalId(rentalId));
    }

    @PostMapping("/rentals/{rentalId}/revisions/calculer")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RevisionLoyerResponse> calculer(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(revisionLoyerService.calculerRevision(rentalId));
    }

    @PostMapping("/revisions/{id}/notifier")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RevisionLoyerResponse> notifier(@PathVariable UUID id) {
        return ResponseEntity.ok(revisionLoyerService.notifierRevision(id));
    }

    @PostMapping("/revisions/{id}/appliquer")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RevisionLoyerResponse> appliquer(@PathVariable UUID id) {
        return ResponseEntity.ok(revisionLoyerService.appliquerRevision(id));
    }

    @PostMapping("/revisions/{id}/annuler")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RevisionLoyerResponse> annuler(@PathVariable UUID id) {
        return ResponseEntity.ok(revisionLoyerService.annulerRevision(id));
    }

    @GetMapping("/revisions/{id}/courrier")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadCourrier(@PathVariable UUID id) throws IOException {
        byte[] pdf = revisionLoyerService.downloadCourrier(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=revision-loyer.pdf")
                .body(pdf);
    }
}
