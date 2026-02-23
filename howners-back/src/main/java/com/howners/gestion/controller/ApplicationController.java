package com.howners.gestion.controller;

import com.howners.gestion.dto.application.ApplicationResponse;
import com.howners.gestion.dto.application.CreateApplicationRequest;
import com.howners.gestion.dto.application.CreateRentalFromApplicationRequest;
import com.howners.gestion.dto.application.ReviewApplicationRequest;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.service.application.ApplicationService;
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
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> submit(@Valid @RequestBody CreateApplicationRequest request) {
        log.info("Submitting application for listing: {}", request.listingId());
        ApplicationResponse application = applicationService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        log.info("Fetching my applications");
        List<ApplicationResponse> applications = applicationService.findMyApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getReceivedApplications() {
        log.info("Fetching received applications");
        List<ApplicationResponse> applications = applicationService.findReceivedApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/listing/{listingId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getByListing(@PathVariable UUID listingId) {
        log.info("Fetching applications for listing: {}", listingId);
        List<ApplicationResponse> applications = applicationService.findByListingId(listingId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable UUID id) {
        log.info("Fetching application: {}", id);
        ApplicationResponse application = applicationService.findById(id);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewApplicationRequest request) {
        log.info("Reviewing application: {} -> {}", id, request.status());
        ApplicationResponse application = applicationService.review(id, request);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> withdraw(@PathVariable UUID id) {
        log.info("Withdrawing application: {}", id);
        ApplicationResponse application = applicationService.withdraw(id);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/{id}/create-rental")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RentalResponse> createRentalFromApplication(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRentalFromApplicationRequest request) {
        log.info("Creating rental from application: {}", id);
        RentalResponse rental = applicationService.createRentalFromApplication(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rental);
    }
}
