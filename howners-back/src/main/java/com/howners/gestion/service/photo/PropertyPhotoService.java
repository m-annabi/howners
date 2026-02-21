package com.howners.gestion.service.photo;

import com.howners.gestion.domain.photo.PropertyPhoto;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.photo.PropertyPhotoResponse;
import com.howners.gestion.dto.photo.ReorderPhotosRequest;
import com.howners.gestion.dto.photo.UpdatePropertyPhotoRequest;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.PropertyPhotoRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PropertyPhotoService {

    private final PropertyPhotoRepository photoRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_PHOTOS_PER_PROPERTY = 5;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /**
     * Upload une photo pour un bien
     */
    @Transactional
    public PropertyPhotoResponse uploadPhoto(UUID propertyId, MultipartFile file, String caption) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien immobilier", "id", propertyId));

        // Vérifier que l'utilisateur est le propriétaire
        if (!property.getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à ajouter des photos pour ce bien");
        }

        // Vérifier la limite de photos
        if (photoRepository.hasReachedPhotoLimit(propertyId)) {
            throw new BadRequestException("Limite maximale de 5 photos atteinte pour ce bien");
        }

        // Valider le fichier
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("La taille du fichier dépasse la limite de 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException("Type de fichier invalide. Seuls les formats JPEG, PNG et WebP sont autorisés");
        }

        // Upload vers MinIO
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename();
        String fileKey = storageService.uploadFile(fileBytes, fileName, contentType);

        // Déterminer l'ordre d'affichage et si c'est la photo principale
        long currentPhotoCount = photoRepository.countByPropertyId(propertyId);
        boolean isPrimary = currentPhotoCount == 0; // Première photo = primary
        int displayOrder = (int) currentPhotoCount;

        // Créer l'entité (sans fileUrl, on le génère à la volée)
        PropertyPhoto photo = PropertyPhoto.builder()
                .property(property)
                .uploader(currentUser)
                .fileName(fileName)
                .fileKey(fileKey)
                .fileUrl(null) // Ne pas stocker l'URL
                .fileSize(file.getSize())
                .mimeType(contentType)
                .caption(caption)
                .displayOrder(displayOrder)
                .isPrimary(isPrimary)
                .build();

        photo = photoRepository.save(photo);
        log.info("Photo uploaded successfully for property {}: {}", propertyId, fileName);

        return toResponse(photo);
    }

    /**
     * Récupère toutes les photos d'un bien
     */
    public List<PropertyPhotoResponse> getPropertyPhotos(UUID propertyId) {
        return photoRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère la photo de couverture d'un bien
     */
    public PropertyPhotoResponse getPrimaryPhoto(UUID propertyId) {
        return photoRepository.findByPropertyIdAndIsPrimaryTrue(propertyId)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Met à jour une photo
     */
    @Transactional
    public PropertyPhotoResponse updatePhoto(UUID propertyId, UUID photoId, UpdatePropertyPhotoRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        PropertyPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        // Vérifier que la photo appartient bien au bien spécifié
        if (!photo.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("Cette photo n'appartient pas à ce bien");
        }

        // Vérifier que l'utilisateur est le propriétaire
        if (!photo.getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier cette photo");
        }

        // Mettre à jour les champs fournis
        if (request.caption() != null) {
            photo.setCaption(request.caption());
        }

        if (request.displayOrder() != null) {
            photo.setDisplayOrder(request.displayOrder());
        }

        if (request.isPrimary() != null && request.isPrimary()) {
            // Si on définit cette photo comme primary, retirer le flag des autres
            photoRepository.clearPrimaryFlagForProperty(propertyId);
            photo.setIsPrimary(true);
        }

        photo = photoRepository.save(photo);
        log.info("Photo {} updated successfully", photoId);

        return toResponse(photo);
    }

    /**
     * Réorganise les photos d'un bien
     */
    @Transactional
    public List<PropertyPhotoResponse> reorderPhotos(UUID propertyId, ReorderPhotosRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien immobilier", "id", propertyId));

        // Vérifier que l'utilisateur est le propriétaire
        if (!property.getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à réorganiser les photos de ce bien");
        }

        // Mettre à jour l'ordre des photos
        for (ReorderPhotosRequest.PhotoOrderItem item : request.photos()) {
            PropertyPhoto photo = photoRepository.findById(item.photoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", item.photoId()));

            // Vérifier que la photo appartient au bien
            if (!photo.getProperty().getId().equals(propertyId)) {
                throw new BadRequestException("La photo " + item.photoId() + " n'appartient pas à ce bien");
            }

            photo.setDisplayOrder(item.displayOrder());
            photoRepository.save(photo);
        }

        log.info("Photos reordered successfully for property {}", propertyId);

        return getPropertyPhotos(propertyId);
    }

    /**
     * Supprime une photo
     */
    @Transactional
    public void deletePhoto(UUID propertyId, UUID photoId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        PropertyPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        // Vérifier que la photo appartient bien au bien spécifié
        if (!photo.getProperty().getId().equals(propertyId)) {
            throw new BadRequestException("Cette photo n'appartient pas à ce bien");
        }

        // Vérifier que l'utilisateur est le propriétaire
        if (!photo.getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer cette photo");
        }

        boolean wasPrimary = photo.getIsPrimary();
        String fileKey = photo.getFileKey();

        // Supprimer de la base de données
        photoRepository.delete(photo);

        // Supprimer de MinIO
        try {
            storageService.deleteFile(fileKey);
            log.info("Photo file deleted from storage: {}", fileKey);
        } catch (Exception e) {
            log.error("Error deleting file from storage: {}", fileKey, e);
            // Ne pas bloquer la suppression si l'effacement du fichier échoue
        }

        // Si c'était la photo primary, définir la première photo restante comme primary
        if (wasPrimary) {
            List<PropertyPhoto> remainingPhotos = photoRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
            if (!remainingPhotos.isEmpty()) {
                PropertyPhoto newPrimary = remainingPhotos.get(0);
                newPrimary.setIsPrimary(true);
                photoRepository.save(newPrimary);
                log.info("New primary photo set: {}", newPrimary.getId());
            }
        }

        log.info("Photo {} deleted successfully", photoId);
    }

    /**
     * Convertit une entité PropertyPhoto en DTO PropertyPhotoResponse
     * Génère l'URL présignée à la volée
     */
    private PropertyPhotoResponse toResponse(PropertyPhoto photo) {
        String fileUrl = storageService.generatePresignedUrl(photo.getFileKey());

        return new PropertyPhotoResponse(
                photo.getId(),
                photo.getProperty().getId(),
                photo.getFileName(),
                fileUrl, // URL présignée générée à la volée
                photo.getFileSize(),
                photo.getMimeType(),
                photo.getCaption(),
                photo.getDisplayOrder(),
                photo.getIsPrimary(),
                photo.getUploader().getId(),
                getFullName(photo.getUploader()),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }

    private boolean isAdmin(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ADMIN;
    }

    private String getFullName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
