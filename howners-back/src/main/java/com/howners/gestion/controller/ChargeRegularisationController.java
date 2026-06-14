package com.howners.gestion.controller;

import com.howners.gestion.dto.rental.RegularisationResponse;
import com.howners.gestion.service.rental.RegularisationChargesService;
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
public class ChargeRegularisationController {

    private final RegularisationChargesService regularisationService;

    @GetMapping("/rentals/{rentalId}/regularisations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegularisationResponse>> getRegularisations(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(regularisationService.findByRentalId(rentalId));
    }

    @PostMapping("/rentals/{rentalId}/regularisations")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RegularisationResponse> calculer(
            @PathVariable UUID rentalId,
            @RequestParam int annee) {
        return ResponseEntity.ok(regularisationService.calculer(rentalId, annee));
    }

    @PostMapping("/regularisations/{id}/envoyer")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RegularisationResponse> envoyer(@PathVariable UUID id) {
        return ResponseEntity.ok(regularisationService.envoyerDecompte(id));
    }

    @PostMapping("/regularisations/{id}/paiement")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RegularisationResponse> creerPaiement(@PathVariable UUID id) {
        return ResponseEntity.ok(regularisationService.creerPaiementComplementaire(id));
    }

    @GetMapping("/regularisations/{id}/decompte")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadDecompte(@PathVariable UUID id) throws IOException {
        byte[] pdf = regularisationService.downloadDecompte(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=decompte-charges.pdf")
                .body(pdf);
    }
}
