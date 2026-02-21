package com.howners.gestion.controller;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.dto.listing.CreateListingRequest;
import com.howners.gestion.dto.listing.ListingResponse;
import com.howners.gestion.service.listing.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<List<ListingResponse>> searchListings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) BigDecimal minSurface,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Boolean furnished,
            @RequestParam(required = false) LocalDate availableFrom,
            @RequestParam(required = false) String sortBy) {
        log.info("Searching listings - search: {}, city: {}, department: {}, postalCode: {}", search, city, department, postalCode);
        List<ListingResponse> listings = listingService.searchPublishedAdvanced(
                search, city, department, postalCode,
                priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished,
                availableFrom, sortBy);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable UUID id) {
        log.info("Fetching listing: {}", id);
        ListingResponse listing = listingService.findById(id);
        return ResponseEntity.ok(listing);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ListingResponse>> getMyListings() {
        log.info("Fetching my listings");
        List<ListingResponse> listings = listingService.findMyListings();
        return ResponseEntity.ok(listings);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        log.info("Creating listing for property: {}", request.propertyId());
        ListingResponse listing = listingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(listing);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable UUID id,
            @Valid @RequestBody CreateListingRequest request) {
        log.info("Updating listing: {}", id);
        ListingResponse listing = listingService.update(id, request);
        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingResponse> publishListing(@PathVariable UUID id) {
        log.info("Publishing listing: {}", id);
        ListingResponse listing = listingService.publish(id);
        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingResponse> pauseListing(@PathVariable UUID id) {
        log.info("Pausing listing: {}", id);
        ListingResponse listing = listingService.pause(id);
        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingResponse> closeListing(@PathVariable UUID id) {
        log.info("Closing listing: {}", id);
        ListingResponse listing = listingService.close(id);
        return ResponseEntity.ok(listing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteListing(@PathVariable UUID id) {
        log.info("Deleting listing: {}", id);
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
