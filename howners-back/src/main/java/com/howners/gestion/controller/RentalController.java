package com.howners.gestion.controller;

import com.howners.gestion.dto.request.CreateRentalRequest;
import com.howners.gestion.dto.request.UpdateRentalRequest;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.service.rental.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<List<RentalResponse>> getAllRentals() {
        return ResponseEntity.ok(rentalService.findAllByCurrentUser());
    }

    @GetMapping("/my-tenants")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<UserResponse>> getMyTenants() {
        return ResponseEntity.ok(rentalService.findMyTenants());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'TENANT', 'ADMIN')")
    public ResponseEntity<RentalResponse> getRental(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RentalResponse> createRental(@Valid @RequestBody CreateRentalRequest request) {
        RentalResponse rental = rentalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rental);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<RentalResponse> updateRental(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRentalRequest request
    ) {
        return ResponseEntity.ok(rentalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteRental(@PathVariable UUID id) {
        rentalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
