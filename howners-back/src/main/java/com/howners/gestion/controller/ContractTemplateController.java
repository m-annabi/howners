package com.howners.gestion.controller;

import com.howners.gestion.domain.contract.ContractTemplate;
import com.howners.gestion.domain.rental.RentalType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.template.*;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.ContractTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contract-templates")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;
    private final UserRepository userRepository;

    /**
     * GET /api/contract-templates - Récupère tous les templates accessibles
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ContractTemplateResponse>> getMyTemplates(
            @RequestParam(required = false) RentalType rentalType) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.info("Fetching templates for user: {} with rentalType: {}", currentUserId, rentalType);

        List<ContractTemplate> templates = contractTemplateService.getMyTemplates(currentUserId, rentalType);
        List<ContractTemplateResponse> responses = templates.stream()
                .map(ContractTemplateResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/contract-templates/{id} - Récupère un template par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractTemplateResponse> getTemplate(@PathVariable UUID id) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.info("Fetching template: {} for user: {}", id, currentUserId);

        ContractTemplate template = contractTemplateService.getMyTemplates(currentUserId, null).stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Template not found or not accessible"));

        return ResponseEntity.ok(ContractTemplateResponse.from(template));
    }

    /**
     * POST /api/contract-templates - Crée un nouveau template
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractTemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Creating new template: {} by user: {}", request.name(), currentUserId);

        ContractTemplate template = contractTemplateService.createTemplate(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractTemplateResponse.from(template));
    }

    /**
     * PUT /api/contract-templates/{id} - Met à jour un template
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Updating template: {} by user: {}", id, currentUserId);

        ContractTemplate template = contractTemplateService.updateTemplate(id, request, currentUser);
        return ResponseEntity.ok(ContractTemplateResponse.from(template));
    }

    /**
     * DELETE /api/contract-templates/{id} - Supprime un template (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Deleting template: {} by user: {}", id, currentUserId);

        contractTemplateService.deleteTemplate(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/contract-templates/{id}/duplicate - Duplique un template
     */
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ContractTemplateResponse> duplicateTemplate(
            @PathVariable UUID id,
            @RequestParam String newName) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Duplicating template: {} as '{}' by user: {}", id, newName, currentUserId);

        ContractTemplate template = contractTemplateService.duplicateTemplate(id, newName, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractTemplateResponse.from(template));
    }

    /**
     * POST /api/contract-templates/{id}/preview - Prévisualise un template rempli
     */
    @PostMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PreviewTemplateResponse> previewTemplate(
            @PathVariable UUID id,
            @RequestParam UUID rentalId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.info("Previewing template: {} with rental: {} by user: {}", id, rentalId, currentUserId);

        PreviewTemplateResponse preview = contractTemplateService.previewTemplate(id, rentalId);
        return ResponseEntity.ok(preview);
    }

    /**
     * POST /api/contract-templates/preview-custom - Prévisualise un contenu personnalisé
     */
    @PostMapping("/preview-custom")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PreviewTemplateResponse> previewCustomContent(
            @Valid @RequestBody PreviewTemplateRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.info("Previewing custom content with rental: {} by user: {}", request.rentalId(), currentUserId);

        PreviewTemplateResponse preview = contractTemplateService.previewCustomContent(
                request.customContent(),
                request.rentalId()
        );
        return ResponseEntity.ok(preview);
    }

    /**
     * GET /api/contract-templates/variables - Récupère toutes les variables disponibles
     */
    @GetMapping("/variables")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<TemplateVariablesResponse> getAvailableVariables() {
        log.info("Fetching available template variables");
        TemplateVariablesResponse variables = contractTemplateService.getAvailableVariables();
        return ResponseEntity.ok(variables);
    }
}
