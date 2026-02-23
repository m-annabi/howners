package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.ContractPublicView;
import com.howners.gestion.dto.contract.SignContractRequest;
import com.howners.gestion.service.contract.ContractESignatureService;
import com.howners.gestion.util.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/contracts")
@RequiredArgsConstructor
@Slf4j
public class PublicContractController {

    private final ContractESignatureService esignatureService;

    @GetMapping("/token/{token}")
    public ResponseEntity<ContractPublicView> getContractByToken(@PathVariable String token) {
        log.info("Public request to view contract by token");
        return ResponseEntity.ok(esignatureService.getContractByToken(token));
    }

    @GetMapping("/token/{token}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(@PathVariable String token) {
        log.info("Public request to download contract PDF by token");
        byte[] pdfBytes = esignatureService.downloadContractPdfByToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=contrat.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/token/{token}/sign")
    public ResponseEntity<Void> signContract(
            @PathVariable String token,
            @Valid @RequestBody SignContractRequest request,
            HttpServletRequest httpRequest) {
        log.info("Public request to sign contract by token");
        String ipAddress = HttpRequestUtils.getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        esignatureService.signContractByToken(
                token,
                request.signatureData(),
                request.signerName(),
                ipAddress,
                userAgent
        );

        return ResponseEntity.ok().build();
    }
}
