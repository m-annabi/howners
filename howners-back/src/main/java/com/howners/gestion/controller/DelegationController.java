package com.howners.gestion.controller;

import com.howners.gestion.dto.user.ApercuCompteResponse;
import com.howners.gestion.dto.user.CreateDelegationRequest;
import com.howners.gestion.dto.user.DelegationResponse;
import com.howners.gestion.service.user.DelegationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delegations")
@RequiredArgsConstructor
@Slf4j
public class DelegationController {

    private final DelegationService delegationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<DelegationResponse>> mesDelegations() {
        return ResponseEntity.ok(delegationService.mesDelegations());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<DelegationResponse> inviter(@Valid @RequestBody CreateDelegationRequest request) {
        return ResponseEntity.ok(delegationService.inviterParEmail(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> revoquer(@PathVariable UUID id) {
        delegationService.revoquer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recues")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DelegationResponse>> delegationsRecues() {
        return ResponseEntity.ok(delegationService.delegationsRecues());
    }

    @GetMapping("/{id}/apercu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApercuCompteResponse> apercu(@PathVariable UUID id) {
        return ResponseEntity.ok(delegationService.getApercuCompte(id));
    }
}
