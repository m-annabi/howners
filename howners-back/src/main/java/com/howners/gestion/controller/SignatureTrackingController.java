package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.SignatureTrackingDashboard;
import com.howners.gestion.service.contract.ContractESignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signature-tracking")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class SignatureTrackingController {

    private final ContractESignatureService esignatureService;

    @GetMapping("/dashboard")
    public ResponseEntity<SignatureTrackingDashboard> getDashboard() {
        log.info("Request for signature tracking dashboard");
        SignatureTrackingDashboard dashboard = esignatureService.getSignatureTrackingDashboard();
        return ResponseEntity.ok(dashboard);
    }
}
