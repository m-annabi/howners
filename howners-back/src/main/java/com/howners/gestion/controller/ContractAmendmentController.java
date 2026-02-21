package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.AmendmentResponse;
import com.howners.gestion.dto.contract.CreateAmendmentRequest;
import com.howners.gestion.service.contract.ContractAmendmentService;
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
@RequestMapping("/api/contracts/{contractId}/amendments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ContractAmendmentController {

    private final ContractAmendmentService amendmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<AmendmentResponse>> getAmendments(@PathVariable UUID contractId) {
        log.info("Fetching amendments for contract: {}", contractId);
        List<AmendmentResponse> amendments = amendmentService.findByContractId(contractId);
        return ResponseEntity.ok(amendments);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<AmendmentResponse> createAmendment(
            @PathVariable UUID contractId,
            @Valid @RequestBody CreateAmendmentRequest request) {
        try {
            log.info("Creating amendment for contract: {}", contractId);
            AmendmentResponse amendment = amendmentService.create(contractId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(amendment);
        } catch (IOException e) {
            log.error("Error creating amendment: {}", e.getMessage());
            throw new RuntimeException("Failed to generate amendment PDF", e);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<AmendmentResponse> getAmendment(
            @PathVariable UUID contractId,
            @PathVariable UUID id) {
        log.info("Fetching amendment: {}", id);
        AmendmentResponse amendment = amendmentService.findById(id);
        return ResponseEntity.ok(amendment);
    }

    @PutMapping("/{id}/sign")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<AmendmentResponse> signAmendment(
            @PathVariable UUID contractId,
            @PathVariable UUID id) {
        log.info("Signing amendment: {}", id);
        AmendmentResponse amendment = amendmentService.sign(id);
        return ResponseEntity.ok(amendment);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID contractId,
            @PathVariable UUID id) throws IOException {
        log.info("Downloading PDF for amendment: {}", id);
        byte[] pdfBytes = amendmentService.downloadPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=avenant-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
