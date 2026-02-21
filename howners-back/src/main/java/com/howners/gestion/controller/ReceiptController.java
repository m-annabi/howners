package com.howners.gestion.controller;

import com.howners.gestion.dto.receipt.ReceiptResponse;
import com.howners.gestion.service.receipt.ReceiptService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<ReceiptResponse>> getAllReceipts() {
        return ResponseEntity.ok(receiptService.findByCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.findById(id));
    }

    @PostMapping("/payment/{paymentId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ReceiptResponse> generateReceipt(@PathVariable UUID paymentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.generateReceipt(paymentId));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable UUID id) throws IOException {
        byte[] pdf = receiptService.downloadReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quittance_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<ReceiptResponse>> getReceiptsByRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(receiptService.findByRentalId(rentalId));
    }
}
