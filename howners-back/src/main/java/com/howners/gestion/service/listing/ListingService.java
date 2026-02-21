package com.howners.gestion.service.listing;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.listing.CreateListingRequest;
import com.howners.gestion.dto.listing.ListingPhotoResponse;
import com.howners.gestion.dto.listing.ListingResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.photo.ListingPhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingService {

    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final ListingPhotoService listingPhotoService;

    @Transactional(readOnly = true)
    public List<ListingResponse> searchPublished(String search) {
        List<Listing> listings;
        if (search != null && !search.isBlank()) {
            listings = listingRepository.searchPublished(search);
        } else {
            listings = listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED);
        }
        return listings.stream().map(this::toResponseWithPhotos).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> searchPublishedWithFilters(String search, String city, String department, String postalCode) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCity = city != null && !city.isBlank();
        boolean hasDepartment = department != null && !department.isBlank();
        boolean hasPostalCode = postalCode != null && !postalCode.isBlank();

        if (!hasSearch && !hasCity && !hasDepartment && !hasPostalCode) {
            return searchPublished(null);
        }

        List<Listing> listings = listingRepository.searchPublishedWithFilters(
                hasSearch ? search : "",
                hasCity ? city : "",
                hasDepartment ? department : "",
                hasPostalCode ? postalCode : ""
        );
        return listings.stream().map(this::toResponseWithPhotos).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> searchPublishedAdvanced(
            String search, String city, String department, String postalCode,
            BigDecimal priceMin, BigDecimal priceMax, PropertyType propertyType,
            BigDecimal minSurface, Integer minBedrooms, Boolean furnished,
            LocalDate availableFrom, String sortBy) {

        boolean hasAny = (search != null && !search.isBlank())
                || (city != null && !city.isBlank())
                || (department != null && !department.isBlank())
                || (postalCode != null && !postalCode.isBlank())
                || priceMin != null || priceMax != null
                || propertyType != null || minSurface != null
                || minBedrooms != null || furnished != null
                || availableFrom != null;

        List<Listing> listings;
        if (!hasAny) {
            listings = listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED);
        } else {
            listings = listingRepository.searchPublishedAdvanced(
                    (search != null && !search.isBlank()) ? search : "",
                    (city != null && !city.isBlank()) ? city : "",
                    (department != null && !department.isBlank()) ? department : "",
                    (postalCode != null && !postalCode.isBlank()) ? postalCode : "",
                    priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished, availableFrom
            );
        }

        List<ListingResponse> results = listings.stream().map(this::toResponseWithPhotos).toList();

        if ("price_asc".equals(sortBy)) {
            results = results.stream()
                    .sorted(Comparator.comparing(ListingResponse::pricePerMonth, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        } else if ("price_desc".equals(sortBy)) {
            results = results.stream()
                    .sorted(Comparator.comparing(ListingResponse::pricePerMonth, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        return results;
    }

    @Transactional(readOnly = true)
    public ListingResponse findById(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        return toResponseWithPhotos(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> findMyListings() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Listing> listings;
        if (user.getRole() == Role.ADMIN) {
            listings = listingRepository.findAll();
        } else {
            listings = listingRepository.findByOwnerId(currentUserId);
        }
        return listings.stream().map(this::toResponseWithPhotos).toList();
    }

    @Transactional
    public ListingResponse create(CreateListingRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Listing listing = Listing.builder()
                .property(property)
                .title(request.title())
                .description(request.description())
                .pricePerNight(request.pricePerNight())
                .pricePerMonth(request.pricePerMonth())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .minStay(request.minStay())
                .maxStay(request.maxStay())
                .amenities(request.amenities())
                .requirements(request.requirements())
                .availableFrom(request.availableFrom())
                .status(ListingStatus.DRAFT)
                .build();

        listing = listingRepository.save(listing);
        log.info("Listing created: {} for property {}", listing.getId(), property.getId());
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse update(UUID id, CreateListingRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPricePerNight(request.pricePerNight());
        listing.setPricePerMonth(request.pricePerMonth());
        if (request.currency() != null) listing.setCurrency(request.currency());
        listing.setMinStay(request.minStay());
        listing.setMaxStay(request.maxStay());
        listing.setAmenities(request.amenities());
        listing.setRequirements(request.requirements());
        listing.setAvailableFrom(request.availableFrom());

        listing = listingRepository.save(listing);
        log.info("Listing updated: {}", id);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse publish(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.PAUSED) {
            throw new BadRequestException("Listing must be DRAFT or PAUSED to publish");
        }

        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);
        log.info("Listing published: {}", id);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse pause(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus(ListingStatus.PAUSED);
        listing = listingRepository.save(listing);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse close(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus(ListingStatus.CLOSED);
        listing = listingRepository.save(listing);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public void delete(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listingRepository.delete(listing);
        log.info("Listing deleted: {}", id);
    }

    private ListingResponse toResponseWithPhotos(Listing listing) {
        List<ListingPhotoResponse> photos = listingPhotoService.getListingPhotos(listing.getId());
        return ListingResponse.from(listing, photos);
    }
}
