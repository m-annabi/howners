package com.howners.gestion.controller;

import com.howners.gestion.dto.inventory.ComparaisonEdlResponse;
import com.howners.gestion.dto.inventory.RetenueDepotRequest;
import com.howners.gestion.service.inventory.EdlComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EdlComparisonController {

    private final EdlComparisonService edlComparisonService;

    @GetMapping("/rentals/{rentalId}/edl/comparaison")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComparaisonEdlResponse> comparer(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(edlComparisonService.comparer(rentalId));
    }

    @PutMapping("/rentals/{rentalId}/edl/comparaison/retenues")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComparaisonEdlResponse> enregistrerRetenues(
            @PathVariable UUID rentalId,
            @Valid @RequestBody RetenueDepotRequest request) {
        return ResponseEntity.ok(edlComparisonService.enregistrerRetenues(rentalId, request));
    }

    @PostMapping("/rentals/{rentalId}/edl/comparaison/valider")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ComparaisonEdlResponse> valider(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(edlComparisonService.valider(rentalId));
    }

    @GetMapping("/edl-comparaisons/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) throws IOException {
        byte[] pdf = edlComparisonService.downloadPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=comparatif-edl.pdf")
                .body(pdf);
    }
}
