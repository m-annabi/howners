package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.ContractPublicView;
import com.howners.gestion.service.contract.ContractESignatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller public pour l'accès aux contrats via token (sans authentification).
 * Permet au locataire de consulter et signer directement le contrat.
 */
@RestController
@RequestMapping("/api/public/contracts")
@RequiredArgsConstructor
@Slf4j
public class PublicContractController {

    private final ContractESignatureService esignatureService;

    /**
     * Récupère un contrat par son token d'accès
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<ContractPublicView> getContractByToken(@PathVariable String token) {
        log.info("Public request to view contract by token");

        try {
            ContractPublicView contract = esignatureService.getContractByToken(token);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            log.error("Failed to get contract by token", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Télécharge le PDF du contrat pour prévisualisation
     */
    @GetMapping("/token/{token}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(@PathVariable String token) {
        log.info("Public request to download contract PDF by token");

        try {
            byte[] pdfBytes = esignatureService.downloadContractPdfByToken(token);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=contrat.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to download contract PDF by token", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Signe le contrat via le token public.
     * Reçoit la signature dessinée (base64 PNG) et l'applique directement sur le PDF.
     */
    @PostMapping("/token/{token}/sign")
    public ResponseEntity<Void> signContract(
            @PathVariable String token,
            @RequestBody SignContractRequest request,
            HttpServletRequest httpRequest) {
        log.info("Public request to sign contract by token");

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            esignatureService.signContractByToken(
                    token,
                    request.signatureData(),
                    request.signerName(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to sign contract by token", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DTO pour la requête de signature
     */
    public record SignContractRequest(String signatureData, String signerName) {}

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
