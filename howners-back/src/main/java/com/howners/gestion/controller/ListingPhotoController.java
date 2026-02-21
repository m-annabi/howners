package com.howners.gestion.controller;

import com.howners.gestion.dto.listing.ListingPhotoResponse;
import com.howners.gestion.dto.photo.ReorderPhotosRequest;
import com.howners.gestion.dto.photo.UpdatePropertyPhotoRequest;
import com.howners.gestion.service.photo.ListingPhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/photos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class ListingPhotoController {

    private final ListingPhotoService photoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingPhotoResponse> uploadPhoto(
            @PathVariable UUID listingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {

        try {
            log.info("Uploading photo for listing {}: {}", listingId, file.getOriginalFilename());
            ListingPhotoResponse photo = photoService.uploadPhoto(listingId, file, caption);
            return ResponseEntity.status(HttpStatus.CREATED).body(photo);
        } catch (IOException e) {
            log.error("Error uploading photo: {}", e.getMessage());
            throw new RuntimeException("Failed to upload photo", e);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ListingPhotoResponse>> getListingPhotos(@PathVariable UUID listingId) {
        log.info("Fetching photos for listing {}", listingId);
        List<ListingPhotoResponse> photos = photoService.getListingPhotos(listingId);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/primary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingPhotoResponse> getPrimaryPhoto(@PathVariable UUID listingId) {
        log.info("Fetching primary photo for listing {}", listingId);
        ListingPhotoResponse photo = photoService.getPrimaryPhoto(listingId);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(photo);
    }

    @PutMapping("/{photoId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ListingPhotoResponse> updatePhoto(
            @PathVariable UUID listingId,
            @PathVariable UUID photoId,
            @RequestBody UpdatePropertyPhotoRequest request) {

        log.info("Updating photo {} for listing {}", photoId, listingId);
        ListingPhotoResponse photo = photoService.updatePhoto(listingId, photoId, request);
        return ResponseEntity.ok(photo);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ListingPhotoResponse>> reorderPhotos(
            @PathVariable UUID listingId,
            @RequestBody ReorderPhotosRequest request) {

        log.info("Reordering photos for listing {}", listingId);
        List<ListingPhotoResponse> photos = photoService.reorderPhotos(listingId, request);
        return ResponseEntity.ok(photos);
    }

    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable UUID listingId,
            @PathVariable UUID photoId) {

        log.info("Deleting photo {} from listing {}", photoId, listingId);
        photoService.deletePhoto(listingId, photoId);
        return ResponseEntity.noContent().build();
    }
}
