package com.howners.gestion.controller;

import com.howners.gestion.dto.payment.CreatePaymentRequest;
import com.howners.gestion.dto.payment.PaymentResponse;
import com.howners.gestion.dto.payment.StripePaymentIntentResponse;
import com.howners.gestion.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.findByCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @PostMapping("/{id}/stripe-intent")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<StripePaymentIntentResponse> createStripePaymentIntent(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.createStripePaymentIntent(id));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.confirmPayment(id));
    }

    @GetMapping("/rental/{rentalId}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRental(@PathVariable UUID rentalId) {
        return ResponseEntity.ok(paymentService.findByRentalId(rentalId));
    }
}
