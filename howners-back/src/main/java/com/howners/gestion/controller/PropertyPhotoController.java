package com.howners.gestion.controller;

import com.howners.gestion.dto.photo.PropertyPhotoResponse;
import com.howners.gestion.dto.photo.ReorderPhotosRequest;
import com.howners.gestion.dto.photo.UpdatePropertyPhotoRequest;
import com.howners.gestion.service.photo.PropertyPhotoService;
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
@RequestMapping("/api/properties/{propertyId}/photos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class PropertyPhotoController {

    private final PropertyPhotoService photoService;

    /**
     * POST /api/properties/{propertyId}/photos - Upload une photo
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyPhotoResponse> uploadPhoto(
            @PathVariable UUID propertyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {

        try {
            log.info("Uploading photo for property {}: {}", propertyId, file.getOriginalFilename());
            PropertyPhotoResponse photo = photoService.uploadPhoto(propertyId, file, caption);
            return ResponseEntity.status(HttpStatus.CREATED).body(photo);
        } catch (IOException e) {
            log.error("Error uploading photo: {}", e.getMessage());
            throw new RuntimeException("Failed to upload photo", e);
        }
    }

    /**
     * GET /api/properties/{propertyId}/photos - Liste des photos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PropertyPhotoResponse>> getPropertyPhotos(@PathVariable UUID propertyId) {
        log.info("Fetching photos for property {}", propertyId);
        List<PropertyPhotoResponse> photos = photoService.getPropertyPhotos(propertyId);
        return ResponseEntity.ok(photos);
    }

    /**
     * GET /api/properties/{propertyId}/photos/primary - Photo de couverture
     */
    @GetMapping("/primary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyPhotoResponse> getPrimaryPhoto(@PathVariable UUID propertyId) {
        log.info("Fetching primary photo for property {}", propertyId);
        PropertyPhotoResponse photo = photoService.getPrimaryPhoto(propertyId);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(photo);
    }

    /**
     * PUT /api/properties/{propertyId}/photos/{photoId} - Mettre à jour une photo
     */
    @PutMapping("/{photoId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PropertyPhotoResponse> updatePhoto(
            @PathVariable UUID propertyId,
            @PathVariable UUID photoId,
            @RequestBody UpdatePropertyPhotoRequest request) {

        log.info("Updating photo {} for property {}", photoId, propertyId);
        PropertyPhotoResponse photo = photoService.updatePhoto(propertyId, photoId, request);
        return ResponseEntity.ok(photo);
    }

    /**
     * POST /api/properties/{propertyId}/photos/reorder - Réorganiser les photos
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<PropertyPhotoResponse>> reorderPhotos(
            @PathVariable UUID propertyId,
            @RequestBody ReorderPhotosRequest request) {

        log.info("Reordering photos for property {}", propertyId);
        List<PropertyPhotoResponse> photos = photoService.reorderPhotos(propertyId, request);
        return ResponseEntity.ok(photos);
    }

    /**
     * DELETE /api/properties/{propertyId}/photos/{photoId} - Supprimer une photo
     */
    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable UUID propertyId,
            @PathVariable UUID photoId) {

        log.info("Deleting photo {} from property {}", photoId, propertyId);
        photoService.deletePhoto(propertyId, photoId);
        return ResponseEntity.noContent().build();
    }
}
