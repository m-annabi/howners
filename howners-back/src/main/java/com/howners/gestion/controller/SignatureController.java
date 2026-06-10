package com.howners.gestion.controller;

import com.howners.gestion.dto.signature.CreateSignatureRequest;
import com.howners.gestion.dto.signature.SignatureResponse;
import com.howners.gestion.service.signature.SignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class SignatureController {

    private final SignatureService signatureService;

    /**
     * POST /api/signatures - Créer une signature
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<SignatureResponse> createSignature(
            @Valid @RequestBody CreateSignatureRequest request,
            HttpServletRequest httpRequest) {

        // Enrichir la requête avec IP et User-Agent
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        CreateSignatureRequest enrichedRequest = new CreateSignatureRequest(
                request.contractId(),
                request.signatureData(),
                ipAddress,
                userAgent
        );

        log.info("Creating signature for contract: {} from IP: {}", request.contractId(), ipAddress);
        SignatureResponse signature = signatureService.createSignature(enrichedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(signature);
    }

    /**
     * GET /api/signatures - Récupérer toutes les signatures de l'utilisateur
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<SignatureResponse>> getMySignatures() {
        log.info("Fetching signatures for current user");
        List<SignatureResponse> signatures = signatureService.getMySignatures();
        return ResponseEntity.ok(signatures);
    }

    /**
     * GET /api/signatures/{id} - Récupérer une signature par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<SignatureResponse> getSignature(@PathVariable UUID id) {
        log.info("Fetching signature: {}", id);
        SignatureResponse signature = signatureService.getSignature(id);
        return ResponseEntity.ok(signature);
    }

    /**
     * GET /api/signatures/contract/{contractId} - Récupérer les signatures d'un contrat
     */
    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<SignatureResponse>> getContractSignatures(@PathVariable UUID contractId) {
        log.info("Fetching signatures for contract: {}", contractId);
        List<SignatureResponse> signatures = signatureService.getContractSignatures(contractId);
        return ResponseEntity.ok(signatures);
    }

    /**
     * Récupère l'adresse IP du client en tenant compte des proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Si plusieurs IPs, prendre la première
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
