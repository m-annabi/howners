package com.howners.gestion.controller;

import com.howners.gestion.dto.rating.CreateOwnerRatingRequest;
import com.howners.gestion.dto.rating.OwnerRatingResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.service.rating.OwnerRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/owner-ratings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class OwnerRatingController {

    private final OwnerRatingService ratingService;

    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<OwnerRatingResponse> create(@Valid @RequestBody CreateOwnerRatingRequest request) {
        return ResponseEntity.ok(ratingService.create(request));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OwnerRatingResponse>> getRatingsForOwner(@PathVariable UUID ownerId) {
        return ResponseEntity.ok(ratingService.getRatingsForOwner(ownerId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<OwnerRatingResponse>> getMyRatings() {
        return ResponseEntity.ok(ratingService.getMyRatings());
    }

    @GetMapping("/owner/{ownerId}/profile")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<UserResponse> getOwnerProfile(@PathVariable UUID ownerId) {
        return ResponseEntity.ok(ratingService.getOwnerProfile(ownerId));
    }
}
