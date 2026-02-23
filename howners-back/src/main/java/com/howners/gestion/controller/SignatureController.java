package com.howners.gestion.controller;

import com.howners.gestion.dto.signature.CreateSignatureRequest;
import com.howners.gestion.dto.signature.SignatureResponse;
import com.howners.gestion.service.signature.SignatureService;
import com.howners.gestion.util.HttpRequestUtils;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<SignatureResponse> createSignature(
            @Valid @RequestBody CreateSignatureRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = HttpRequestUtils.getClientIpAddress(httpRequest);
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

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<SignatureResponse>> getMySignatures() {
        return ResponseEntity.ok(signatureService.getMySignatures());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<SignatureResponse> getSignature(@PathVariable UUID id) {
        return ResponseEntity.ok(signatureService.getSignature(id));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('TENANT', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<SignatureResponse>> getContractSignatures(@PathVariable UUID contractId) {
        return ResponseEntity.ok(signatureService.getContractSignatures(contractId));
    }
}
