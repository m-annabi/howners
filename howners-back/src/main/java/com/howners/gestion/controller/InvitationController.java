package com.howners.gestion.controller;

import com.howners.gestion.domain.search.InvitationStatus;
import com.howners.gestion.dto.search.CreateInvitationRequest;
import com.howners.gestion.dto.search.InvitationResponse;
import com.howners.gestion.service.search.InvitationService;
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
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<InvitationResponse> invite(@Valid @RequestBody CreateInvitationRequest request) {
        log.info("Sending invitation to tenant {} for listing {}", request.tenantId(), request.listingId());
        InvitationResponse response = invitationService.invite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<List<InvitationResponse>> getReceivedInvitations() {
        log.info("Fetching received invitations");
        return ResponseEntity.ok(invitationService.getMyReceivedInvitations());
    }

    @GetMapping("/sent")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<InvitationResponse>> getSentInvitations() {
        log.info("Fetching sent invitations");
        return ResponseEntity.ok(invitationService.getMySentInvitations());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam InvitationStatus status) {
        log.info("Updating invitation {} status to {}", id, status);
        return ResponseEntity.ok(invitationService.updateStatus(id, status));
    }
}
