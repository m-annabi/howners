package com.howners.gestion.controller;

import com.howners.gestion.dto.invoice.CreateInvoiceRequest;
import com.howners.gestion.dto.invoice.InvoiceResponse;
import com.howners.gestion.service.invoice.InvoiceService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.findByCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) throws IOException {
        byte[] pdf = invoiceService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(invoiceService.findByRentalId(rentalId));
    }
}
