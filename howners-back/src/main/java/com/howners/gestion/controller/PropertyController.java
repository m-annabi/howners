package com.howners.gestion.controller;

import com.howners.gestion.dto.request.CreatePropertyRequest;
import com.howners.gestion.dto.request.UpdatePropertyRequest;
import com.howners.gestion.dto.response.PropertyResponse;
import com.howners.gestion.service.property.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PropertyResponse>> getAllProperties() {
        return ResponseEntity.ok(propertyService.findAllByCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyResponse> getProperty(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyResponse> createProperty(@Valid @RequestBody CreatePropertyRequest request) {
        PropertyResponse property = propertyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(property);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        return ResponseEntity.ok(propertyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
