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

/**
 * Controller pour la gestion des signatures électroniques de contrats (endpoints authentifiés)
 */
@RestController
@RequestMapping("/api/contracts/{contractId}/esignature")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class ContractESignatureController {

    private final ContractESignatureService esignatureService;

    /**
     * Envoie un contrat pour signature électronique
     *
     * POST /api/contracts/{contractId}/esignature/send
     */
    @PostMapping("/send")
    public ResponseEntity<SignatureRequestResponse> sendForSignature(
            @PathVariable UUID contractId) {
        log.info("Request to send contract {} for signature", contractId);

        try {
            SignatureRequestResponse response = esignatureService.sendContractForSignature(contractId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send contract for signature", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Récupère le statut d'une demande de signature
     *
     * GET /api/contracts/{contractId}/esignature/status
     */
    @GetMapping("/status")
    public ResponseEntity<SignatureRequestResponse> getSignatureStatus(
            @PathVariable UUID contractId) {
        log.info("Request to get signature status for contract {}", contractId);

        SignatureRequestResponse response = esignatureService.getSignatureStatus(contractId);
        return ResponseEntity.ok(response);
    }

    /**
     * Renvoie une demande de signature
     *
     * POST /api/contracts/{contractId}/esignature/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<Void> resendSignatureRequest(
            @PathVariable UUID contractId,
            @RequestParam UUID signatureRequestId) {
        log.info("Request to resend signature request {} for contract {}", signatureRequestId, contractId);

        try {
            esignatureService.resendSignatureRequest(signatureRequestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to resend signature request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Annule une demande de signature
     *
     * DELETE /api/contracts/{contractId}/esignature/cancel
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<Void> cancelSignatureRequest(
            @PathVariable UUID contractId,
            @RequestParam UUID signatureRequestId) {
        log.info("Request to cancel signature request {} for contract {}", signatureRequestId, contractId);

        try {
            esignatureService.cancelSignatureRequest(signatureRequestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to cancel signature request", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Envoie un contrat pour multi-signature
     *
     * POST /api/contracts/{contractId}/esignature/multi-sign
     */
    @PostMapping("/multi-sign")
    public ResponseEntity<List<SignatureRequestResponse>> sendForMultiSignature(
            @PathVariable UUID contractId,
            @Valid @RequestBody List<SignerInfo> signers) {
        log.info("Request to send contract {} for multi-signature with {} signers", contractId, signers.size());
        List<SignatureRequestResponse> responses = esignatureService.sendContractForMultiSignature(contractId, signers);
        return ResponseEntity.ok(responses);
    }

}
