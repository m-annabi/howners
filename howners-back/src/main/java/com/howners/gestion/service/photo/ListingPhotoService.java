package com.howners.gestion.service.photo;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingPhoto;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.listing.ListingPhotoResponse;
import com.howners.gestion.dto.photo.ReorderPhotosRequest;
import com.howners.gestion.dto.photo.UpdatePropertyPhotoRequest;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingPhotoRepository;
import com.howners.gestion.repository.ListingRepository;
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
public class ListingPhotoService {

    private final ListingPhotoRepository photoRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_PHOTOS_PER_LISTING = 5;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Transactional
    public ListingPhotoResponse uploadPhoto(UUID listingId, MultipartFile file, String caption) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce", "id", listingId));

        // Vérifier que l'utilisateur est le propriétaire du bien lié à l'annonce
        if (!listing.getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à ajouter des photos pour cette annonce");
        }

        // Vérifier la limite de photos
        if (photoRepository.hasReachedPhotoLimit(listingId)) {
            throw new BadRequestException("Limite maximale de 5 photos atteinte pour cette annonce");
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
        long currentPhotoCount = photoRepository.countByListingId(listingId);
        boolean isPrimary = currentPhotoCount == 0;
        int displayOrder = (int) currentPhotoCount;

        ListingPhoto photo = ListingPhoto.builder()
                .listing(listing)
                .uploader(currentUser)
                .fileName(fileName)
                .fileKey(fileKey)
                .filePath(fileKey)
                .fileSize(file.getSize())
                .mimeType(contentType)
                .caption(caption)
                .displayOrder(displayOrder)
                .isPrimary(isPrimary)
                .build();

        photo = photoRepository.save(photo);
        log.info("Photo uploaded successfully for listing {}: {}", listingId, fileName);

        return toResponse(photo);
    }

    public List<ListingPhotoResponse> getListingPhotos(UUID listingId) {
        return photoRepository.findByListingIdOrderByDisplayOrderAsc(listingId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ListingPhotoResponse getPrimaryPhoto(UUID listingId) {
        return photoRepository.findByListingIdAndIsPrimaryTrue(listingId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public ListingPhotoResponse updatePhoto(UUID listingId, UUID photoId, UpdatePropertyPhotoRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        ListingPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        if (!photo.getListing().getId().equals(listingId)) {
            throw new BadRequestException("Cette photo n'appartient pas à cette annonce");
        }

        if (!photo.getListing().getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier cette photo");
        }

        if (request.caption() != null) {
            photo.setCaption(request.caption());
        }

        if (request.displayOrder() != null) {
            photo.setDisplayOrder(request.displayOrder());
        }

        if (request.isPrimary() != null && request.isPrimary()) {
            photoRepository.clearPrimaryFlagForListing(listingId);
            photo.setIsPrimary(true);
        }

        photo = photoRepository.save(photo);
        log.info("Photo {} updated successfully", photoId);

        return toResponse(photo);
    }

    @Transactional
    public List<ListingPhotoResponse> reorderPhotos(UUID listingId, ReorderPhotosRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce", "id", listingId));

        if (!listing.getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à réorganiser les photos de cette annonce");
        }

        for (ReorderPhotosRequest.PhotoOrderItem item : request.photos()) {
            ListingPhoto photo = photoRepository.findById(item.photoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", item.photoId()));

            if (!photo.getListing().getId().equals(listingId)) {
                throw new BadRequestException("La photo " + item.photoId() + " n'appartient pas à cette annonce");
            }

            photo.setDisplayOrder(item.displayOrder());
            photoRepository.save(photo);
        }

        log.info("Photos reordered successfully for listing {}", listingId);

        return getListingPhotos(listingId);
    }

    @Transactional
    public void deletePhoto(UUID listingId, UUID photoId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        ListingPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        if (!photo.getListing().getId().equals(listingId)) {
            throw new BadRequestException("Cette photo n'appartient pas à cette annonce");
        }

        if (!photo.getListing().getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer cette photo");
        }

        boolean wasPrimary = photo.getIsPrimary();
        String fileKey = photo.getFileKey();

        photoRepository.delete(photo);

        // Supprimer de MinIO
        if (fileKey != null && !fileKey.isEmpty()) {
            try {
                storageService.deleteFile(fileKey);
                log.info("Photo file deleted from storage: {}", fileKey);
            } catch (Exception e) {
                log.error("Error deleting file from storage: {}", fileKey, e);
            }
        }

        // Si c'était la photo primary, définir la première photo restante comme primary
        if (wasPrimary) {
            List<ListingPhoto> remainingPhotos = photoRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
            if (!remainingPhotos.isEmpty()) {
                ListingPhoto newPrimary = remainingPhotos.get(0);
                newPrimary.setIsPrimary(true);
                photoRepository.save(newPrimary);
                log.info("New primary photo set: {}", newPrimary.getId());
            }
        }

        log.info("Photo {} deleted successfully", photoId);
    }

    private ListingPhotoResponse toResponse(ListingPhoto photo) {
        String fileUrl = (photo.getFileKey() != null && !photo.getFileKey().isEmpty())
                ? storageService.generatePresignedUrl(photo.getFileKey())
                : photo.getFilePath();

        return new ListingPhotoResponse(
                photo.getId(),
                photo.getListing().getId(),
                photo.getFileName(),
                fileUrl,
                photo.getFileSize(),
                photo.getMimeType(),
                photo.getCaption(),
                photo.getDisplayOrder(),
                photo.getIsPrimary(),
                photo.getUploader() != null ? photo.getUploader().getId() : null,
                photo.getUploader() != null ? getFullName(photo.getUploader()) : null,
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
