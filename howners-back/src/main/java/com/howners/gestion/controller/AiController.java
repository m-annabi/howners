package com.howners.gestion.controller;

import com.howners.gestion.dto.ai.DraftLeaseRequest;
import com.howners.gestion.dto.ai.DraftLeaseResponse;
import com.howners.gestion.service.ai.AiLeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AiController {

    private final AiLeaseService aiLeaseService;

    @PostMapping("/draft-lease")
    public ResponseEntity<DraftLeaseResponse> draftLease(@Valid @RequestBody DraftLeaseRequest request) {
        return ResponseEntity.ok(aiLeaseService.draft(request));
    }
}
