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
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.photo.ListingPhotoService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final FeatureGateService featureGateService;

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
            LocalDate availableFrom, String sortBy,
            BigDecimal nearLat, BigDecimal nearLng, BigDecimal radiusKm,
            String dpeMax, Boolean parking, String exterior,
            Boolean elevator, Boolean pmr, Boolean cellar) {

        // Bounding box for radius search. Conservative (square containing the circle);
        // an exact haversine pass below refines to the actual disk.
        boolean hasRadius = nearLat != null && nearLng != null && radiusKm != null
                && radiusKm.signum() > 0;

        BigDecimal latMin = null, latMax = null, lngMin = null, lngMax = null;
        if (hasRadius) {
            double r = radiusKm.doubleValue();
            double lat = nearLat.doubleValue();
            double latDelta = r / 111.0;
            double lngDelta = r / (111.0 * Math.max(0.01, Math.cos(Math.toRadians(lat))));
            latMin = BigDecimal.valueOf(lat - latDelta);
            latMax = BigDecimal.valueOf(lat + latDelta);
            lngMin = BigDecimal.valueOf(nearLng.doubleValue() - lngDelta);
            lngMax = BigDecimal.valueOf(nearLng.doubleValue() + lngDelta);
        }

        boolean hasAny = (search != null && !search.isBlank())
                || (city != null && !city.isBlank())
                || (department != null && !department.isBlank())
                || (postalCode != null && !postalCode.isBlank())
                || priceMin != null || priceMax != null
                || propertyType != null || minSurface != null
                || minBedrooms != null || furnished != null
                || availableFrom != null
                || hasRadius
                || (dpeMax != null && !dpeMax.isBlank())
                || Boolean.TRUE.equals(parking)
                || (exterior != null && !exterior.isBlank())
                || Boolean.TRUE.equals(elevator)
                || Boolean.TRUE.equals(pmr)
                || Boolean.TRUE.equals(cellar);

        List<Listing> listings;
        if (!hasAny) {
            listings = listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED);
        } else {
            listings = listingRepository.searchPublishedAdvanced(
                    (search != null && !search.isBlank()) ? search : "",
                    (city != null && !city.isBlank()) ? city : "",
                    (department != null && !department.isBlank()) ? department : "",
                    (postalCode != null && !postalCode.isBlank()) ? postalCode : "",
                    priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished, availableFrom,
                    latMin, latMax, lngMin, lngMax
            );
        }

        if (hasRadius) {
            final double lat = nearLat.doubleValue();
            final double lng = nearLng.doubleValue();
            final double rKm = radiusKm.doubleValue();
            listings = listings.stream()
                    .filter(l -> {
                        BigDecimal pLat = l.getProperty().getLatitude();
                        BigDecimal pLng = l.getProperty().getLongitude();
                        if (pLat == null || pLng == null) return false;
                        return haversineKm(lat, lng, pLat.doubleValue(), pLng.doubleValue()) <= rKm;
                    })
                    .toList();
        }

        // Post-filtres confort (comme le rayon : appliqués après la requête)
        if (dpeMax != null && !dpeMax.isBlank()) {
            final String max = dpeMax.trim().toUpperCase();
            listings = listings.stream()
                    .filter(l -> {
                        String dpe = l.getProperty().getDpeRating();
                        return dpe != null && !dpe.isBlank()
                                && dpe.trim().toUpperCase().compareTo(max) <= 0;
                    })
                    .toList();
        }
        if (Boolean.TRUE.equals(parking)) {
            listings = listings.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getProperty().getHasParking())
                            || hasAmenity(l, "parking"))
                    .toList();
        }
        if (exterior != null && !exterior.isBlank()) {
            final String ext = exterior;
            listings = listings.stream()
                    .filter(l -> switch (ext) {
                        case "garden" -> hasAmenity(l, "jardin");
                        case "balcony_terrace" -> hasAmenity(l, "balcon") || hasAmenity(l, "terrasse");
                        default -> hasAmenity(l, "balcon") || hasAmenity(l, "terrasse") || hasAmenity(l, "jardin");
                    })
                    .toList();
        }
        if (Boolean.TRUE.equals(elevator)) {
            listings = listings.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getProperty().getHasElevator())
                            || hasAmenity(l, "ascenseur"))
                    .toList();
        }
        if (Boolean.TRUE.equals(pmr)) {
            listings = listings.stream()
                    .filter(l -> hasAmenity(l, "acces_pmr"))
                    .toList();
        }
        if (Boolean.TRUE.equals(cellar)) {
            listings = listings.stream()
                    .filter(l -> hasAmenity(l, "cave"))
                    .toList();
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
        } else if ("distance_asc".equals(sortBy) && hasRadius) {
            final double lat = nearLat.doubleValue();
            final double lng = nearLng.doubleValue();
            results = results.stream()
                    .sorted(Comparator.comparingDouble(r -> distanceForResponse(r, lat, lng)))
                    .toList();
        }

        return results;
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> searchPublishedAdvanced(
            String search, String city, String department, String postalCode,
            BigDecimal priceMin, BigDecimal priceMax, PropertyType propertyType,
            BigDecimal minSurface, Integer minBedrooms, Boolean furnished,
            LocalDate availableFrom, String sortBy,
            BigDecimal nearLat, BigDecimal nearLng, BigDecimal radiusKm,
            String dpeMax, Boolean parking, String exterior,
            Boolean elevator, Boolean pmr, Boolean cellar,
            Pageable pageable) {

        // When geo-radius, post-filters or custom sort are active, fall back to the
        // non-paginated path because post-fetch filtering / re-sorting is incompatible
        // with DB-level pagination.
        boolean hasRadius = nearLat != null && nearLng != null && radiusKm != null
                && radiusKm.signum() > 0;
        boolean hasCustomSort = sortBy != null && !sortBy.isBlank();
        boolean hasPostFilter = (dpeMax != null && !dpeMax.isBlank())
                || Boolean.TRUE.equals(parking)
                || (exterior != null && !exterior.isBlank())
                || Boolean.TRUE.equals(elevator)
                || Boolean.TRUE.equals(pmr)
                || Boolean.TRUE.equals(cellar);

        if (hasRadius || hasCustomSort || hasPostFilter) {
            List<ListingResponse> all = searchPublishedAdvanced(
                    search, city, department, postalCode,
                    priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished,
                    availableFrom, sortBy, nearLat, nearLng, radiusKm,
                    dpeMax, parking, exterior, elevator, pmr, cellar);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), all.size());
            List<ListingResponse> pageContent = start >= all.size()
                    ? List.of()
                    : all.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, all.size());
        }

        BigDecimal latMin = null, latMax = null, lngMin = null, lngMax = null;

        boolean hasAny = (search != null && !search.isBlank())
                || (city != null && !city.isBlank())
                || (department != null && !department.isBlank())
                || (postalCode != null && !postalCode.isBlank())
                || priceMin != null || priceMax != null
                || propertyType != null || minSurface != null
                || minBedrooms != null || furnished != null
                || availableFrom != null;

        Page<Listing> listings;
        if (!hasAny) {
            listings = listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED, pageable);
        } else {
            listings = listingRepository.searchPublishedAdvanced(
                    (search != null && !search.isBlank()) ? search : "",
                    (city != null && !city.isBlank()) ? city : "",
                    (department != null && !department.isBlank()) ? department : "",
                    (postalCode != null && !postalCode.isBlank()) ? postalCode : "",
                    priceMin, priceMax, propertyType, minSurface, minBedrooms, furnished, availableFrom,
                    latMin, latMax, lngMin, lngMax,
                    pageable
            );
        }

        return listings.map(this::toResponseWithPhotos);
    }

    private static boolean hasAmenity(Listing l, String key) {
        String amenities = l.getAmenities();
        return amenities != null && amenities.toLowerCase().contains(key);
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                  * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    private double distanceForResponse(ListingResponse r, double lat, double lng) {
        BigDecimal pLat = r.propertyLatitude();
        BigDecimal pLng = r.propertyLongitude();
        if (pLat == null || pLng == null) return Double.POSITIVE_INFINITY;
        return haversineKm(lat, lng, pLat.doubleValue(), pLng.doubleValue());
    }

    @Transactional(readOnly = true)
    public ListingResponse findById(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        // Endpoint public : une annonce non publiée ne doit être visible que de son
        // propriétaire (ou d'un admin), pas d'un visiteur anonyme ni d'un tiers.
        if (listing.getStatus() != ListingStatus.PUBLISHED && !isOwnerOrAdmin(listing)) {
            throw new ResourceNotFoundException("Listing not found");
        }
        return toResponseWithPhotos(listing);
    }

    /** Vrai si l'utilisateur courant est le propriétaire du bien de l'annonce, ou un admin. */
    private boolean isOwnerOrAdmin(Listing listing) {
        UUID currentUserId = AuthService.getCurrentUserIdOrNull();
        if (currentUserId == null) return false;
        UUID ownerId = listing.getProperty() != null && listing.getProperty().getOwner() != null
                ? listing.getProperty().getOwner().getId() : null;
        if (currentUserId.equals(ownerId)) return true;
        return userRepository.findById(currentUserId)
                .map(u -> u.getRole() == Role.ADMIN).orElse(false);
    }

    /** Charge une annonce et exige que l'utilisateur courant en soit propriétaire (ou admin). */
    private Listing requireOwnedListing(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (!isOwnerOrAdmin(listing)) {
            throw new ForbiddenException("Cette annonce ne vous appartient pas.");
        }
        return listing;
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

    @Transactional(readOnly = true)
    public Page<ListingResponse> findMyListings(Pageable pageable) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Listing> listings;
        if (user.getRole() == Role.ADMIN) {
            listings = listingRepository.findAll(pageable);
        } else {
            listings = listingRepository.findByOwnerId(currentUserId, pageable);
        }
        return listings.map(this::toResponseWithPhotos);
    }

    @Transactional
    public ListingResponse create(CreateListingRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        featureGateService.assertCanCreate(currentUserId, "LISTINGS");

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        // On ne peut publier une annonce que pour SON propre bien.
        if (property.getOwner() == null || !currentUserId.equals(property.getOwner().getId())) {
            throw new ForbiddenException("Ce bien ne vous appartient pas.");
        }

        Listing listing = Listing.builder()
                .property(property)
                .title(request.title())
                .description(request.description())
                .pricePerNight(request.pricePerNight())
                .pricePerMonth(request.pricePerMonth())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .minStay(request.minStay())
                .maxStay(request.maxStay())
                .amenities(toJson(request.amenities()))
                .requirements(toJson(request.requirements()))
                .availableFrom(request.availableFrom())
                .status(ListingStatus.DRAFT)
                .build();

        listing = listingRepository.save(listing);
        log.info("Listing created: {} for property {}", listing.getId(), property.getId());
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse update(UUID id, CreateListingRequest request) {
        Listing listing = requireOwnedListing(id);

        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPricePerNight(request.pricePerNight());
        listing.setPricePerMonth(request.pricePerMonth());
        if (request.currency() != null) listing.setCurrency(request.currency());
        listing.setMinStay(request.minStay());
        listing.setMaxStay(request.maxStay());
        listing.setAmenities(toJson(request.amenities()));
        listing.setRequirements(toJson(request.requirements()));
        listing.setAvailableFrom(request.availableFrom());

        listing = listingRepository.save(listing);
        log.info("Listing updated: {}", id);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse publish(UUID id) {
        Listing listing = requireOwnedListing(id);

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
        Listing listing = requireOwnedListing(id);
        listing.setStatus(ListingStatus.PAUSED);
        listing = listingRepository.save(listing);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public ListingResponse close(UUID id) {
        Listing listing = requireOwnedListing(id);
        listing.setStatus(ListingStatus.CLOSED);
        listing = listingRepository.save(listing);
        return toResponseWithPhotos(listing);
    }

    @Transactional
    public void delete(UUID id) {
        Listing listing = requireOwnedListing(id);
        listingRepository.delete(listing);
        log.info("Listing deleted: {}", id);
    }

    private ListingResponse toResponseWithPhotos(Listing listing) {
        List<ListingPhotoResponse> photos = listingPhotoService.getListingPhotos(listing.getId());
        return ListingResponse.from(listing, photos);
    }

    /** Sérialise la liste en JSON pour la colonne texte (null si vide) — le filtre plein-texte des recherches continue de fonctionner sur la chaîne. */
    private static String toJson(java.util.List<String> values) {
        if (values == null || values.isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(values);
        } catch (Exception e) {
            return String.join(",", values);
        }
    }
}
