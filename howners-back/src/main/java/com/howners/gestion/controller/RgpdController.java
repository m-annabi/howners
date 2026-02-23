package com.howners.gestion.controller;

import com.howners.gestion.dto.audit.ConsentRequest;
import com.howners.gestion.dto.audit.ConsentResponse;
import com.howners.gestion.dto.audit.UserDataExportResponse;
import com.howners.gestion.service.rgpd.RgpdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rgpd")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RgpdController {

    private final RgpdService rgpdService;

    @GetMapping("/export")
    public ResponseEntity<UserDataExportResponse> exportData() {
        return ResponseEntity.ok(rgpdService.exportUserData());
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportDataAsPdf() throws IOException {
        byte[] pdf = rgpdService.exportUserDataAsPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export-rgpd.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/erasure")
    public ResponseEntity<Void> requestErasure() {
        rgpdService.anonymizeUser();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/consent")
    public ResponseEntity<ConsentResponse> recordConsent(@Valid @RequestBody ConsentRequest request) {
        return ResponseEntity.ok(rgpdService.recordConsent(request));
    }

    @GetMapping("/consent")
    public ResponseEntity<List<ConsentResponse>> getConsents() {
        return ResponseEntity.ok(rgpdService.getUserConsents());
    }

    @DeleteMapping("/consent/{id}")
    public ResponseEntity<Void> deleteConsent(@PathVariable UUID id) {
        rgpdService.deleteConsent(id);
        return ResponseEntity.noContent().build();
    }
}
