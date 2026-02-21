package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.ContractResponse;
import com.howners.gestion.dto.contract.ContractVersionResponse;
import com.howners.gestion.dto.contract.CreateContractRequest;
import com.howners.gestion.dto.contract.UpdateContractRequest;
import com.howners.gestion.service.contract.ContractService;
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
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ContractController {

    private final ContractService contractService;

    /**
     * POST /api/contracts - Créer un nouveau contrat
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody CreateContractRequest request) {
        try {
            log.info("Creating contract for rental: {}", request.rentalId());
            ContractResponse contract = contractService.createContract(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(contract);
        } catch (IOException e) {
            log.error("Error creating contract: {}", e.getMessage());
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    /**
     * GET /api/contracts - Récupérer tous les contrats de l'utilisateur
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'TENANT')")
    public ResponseEntity<List<ContractResponse>> getMyContracts() {
        log.info("Fetching contracts for current user");
        List<ContractResponse> contracts = contractService.getMyContracts();
        return ResponseEntity.ok(contracts);
    }

    /**
     * GET /api/contracts/{id} - Récupérer un contrat par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<ContractResponse> getContract(@PathVariable UUID id) {
        log.info("Fetching contract: {}", id);
        ContractResponse contract = contractService.getContract(id);
        return ResponseEntity.ok(contract);
    }

    /**
     * PUT /api/contracts/{id} - Mettre à jour un contrat
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractResponse> updateContract(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContractRequest request) {
        try {
            log.info("Updating contract: {}", id);
            ContractResponse contract = contractService.updateContract(id, request);
            return ResponseEntity.ok(contract);
        } catch (IOException e) {
            log.error("Error updating contract: {}", e.getMessage());
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    /**
     * GET /api/contracts/{id}/pdf - Télécharger le PDF du contrat
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        try {
            log.info("Downloading PDF for contract: {}", id);
            byte[] pdfBytes = contractService.downloadPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contrat-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (IOException e) {
            log.error("Error downloading contract PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to download contract PDF", e);
        }
    }

    /**
     * DELETE /api/contracts/{id} - Supprimer un contrat (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID id) {
        log.info("Deleting contract: {}", id);
        contractService.deleteContract(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/contracts/rental/{rentalId} - Récupérer les contrats d'une location
     */
    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'TENANT')")
    public ResponseEntity<List<ContractResponse>> getContractsByRental(@PathVariable UUID rentalId) {
        log.info("Fetching contracts for rental: {}", rentalId);
        List<ContractResponse> contracts = contractService.getContractsByRental(rentalId);
        return ResponseEntity.ok(contracts);
    }

    /**
     * GET /api/contracts/{id}/versions - Récupérer toutes les versions d'un contrat
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'TENANT')")
    public ResponseEntity<List<ContractVersionResponse>> getContractVersions(@PathVariable UUID id) {
        log.info("Fetching versions for contract: {}", id);
        List<ContractVersionResponse> versions = contractService.getContractVersions(id);
        return ResponseEntity.ok(versions);
    }
}
