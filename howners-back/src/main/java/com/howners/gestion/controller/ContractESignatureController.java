package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.SignatureRequestResponse;
import com.howners.gestion.dto.contract.SignerInfo;
import com.howners.gestion.service.contract.ContractESignatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts/{contractId}/esignature")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class ContractESignatureController {

    private final ContractESignatureService esignatureService;

    @PostMapping("/send")
    public ResponseEntity<SignatureRequestResponse> sendForSignature(@PathVariable UUID contractId) {
        log.info("Request to send contract {} for signature", contractId);
        return ResponseEntity.ok(esignatureService.sendContractForSignature(contractId));
    }

    @GetMapping("/status")
    public ResponseEntity<SignatureRequestResponse> getSignatureStatus(@PathVariable UUID contractId) {
        return ResponseEntity.ok(esignatureService.getSignatureStatus(contractId));
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resendSignatureRequest(
            @PathVariable UUID contractId,
            @RequestParam UUID signatureRequestId) {
        log.info("Request to resend signature request {} for contract {}", signatureRequestId, contractId);
        esignatureService.resendSignatureRequest(signatureRequestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<Void> cancelSignatureRequest(
            @PathVariable UUID contractId,
            @RequestParam UUID signatureRequestId) {
        log.info("Request to cancel signature request {} for contract {}", signatureRequestId, contractId);
        esignatureService.cancelSignatureRequest(signatureRequestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/multi-sign")
    public ResponseEntity<List<SignatureRequestResponse>> sendForMultiSignature(
            @PathVariable UUID contractId,
            @Valid @RequestBody List<SignerInfo> signers) {
        log.info("Request to send contract {} for multi-signature with {} signers", contractId, signers.size());
        return ResponseEntity.ok(esignatureService.sendContractForMultiSignature(contractId, signers));
    }
}
