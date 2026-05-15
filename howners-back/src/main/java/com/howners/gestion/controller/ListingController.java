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
    public ResponseEntity<?> searchListings(
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
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) BigDecimal nearLat,
            @RequestParam(required = false) BigDecimal nearLng,
            @RequestParam(required = false) BigDecimal radiusKm,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("Searching listings - search: {}, city: {}, near: {},{} r={}km page={} size={}",
                search, city, nearLat, nearLng, radiusKm, page, size);
        List<ListingResponse> listings = listingService.searchPublishedAdvanced(
                search, city, department, postalCode,
                priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished,
                availableFrom, sortBy,
                nearLat, nearLng, radiusKm);

        // Backwards-compatible: without page/size, keep returning the bare list so the
        // existing front-end isn't broken. With page/size, return a paged envelope.
        if (page == null && size == null) {
            return ResponseEntity.ok(listings);
        }

        int pageIdx = page != null ? Math.max(0, page) : 0;
        int pageSize = size != null ? Math.max(1, Math.min(size, 100)) : 20;
        int from = Math.min(pageIdx * pageSize, listings.size());
        int to = Math.min(from + pageSize, listings.size());
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) listings.size() / pageSize) : 1;

        return ResponseEntity.ok(new com.howners.gestion.dto.listing.PagedListingsResponse(
                listings.subList(from, to),
                pageIdx,
                pageSize,
                listings.size(),
                totalPages
        ));
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
