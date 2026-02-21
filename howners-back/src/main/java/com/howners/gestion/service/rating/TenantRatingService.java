package com.howners.gestion.service.rating;

import com.howners.gestion.domain.rating.TenantRating;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rating.*;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.TenantRatingRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRatingService {

    private final TenantRatingRepository tenantRatingRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;

    @Transactional(readOnly = true)
    public List<TenantRatingResponse> findByTenantId(UUID tenantId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = getCurrentUser(currentUserId);

        // OWNER can only see ratings for tenants of their properties
        if (currentUser.getRole() == Role.OWNER) {
            validateOwnerAccessToTenant(currentUserId, tenantId);
        }

        return tenantRatingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(TenantRatingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantRatingSummaryResponse getSummaryByTenantId(UUID tenantId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = getCurrentUser(currentUserId);

        if (currentUser.getRole() == Role.OWNER) {
            validateOwnerAccessToTenant(currentUserId, tenantId);
        }

        Object[] raw = tenantRatingRepository.getAverageRatingsByTenantId(tenantId);
        // JPA may wrap the result row in an outer Object[]
        Object[] result = (raw.length == 1 && raw[0] instanceof Object[]) ? (Object[]) raw[0] : raw;

        if (result == null || result[0] == null) {
            return new TenantRatingSummaryResponse(null, null, null, null, 0L);
        }

        return new TenantRatingSummaryResponse(
                ((Number) result[0]).doubleValue(),
                ((Number) result[1]).doubleValue(),
                ((Number) result[2]).doubleValue(),
                ((Number) result[3]).doubleValue(),
                ((Number) result[4]).longValue()
        );
    }

    @Transactional(readOnly = true)
    public List<TenantRatingResponse> findByRentalId(UUID rentalId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = getCurrentUser(currentUserId);

        if (currentUser.getRole() == Role.OWNER) {
            Rental rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
            if (!rental.getProperty().getOwner().getId().equals(currentUserId)) {
                throw new ForbiddenException("You don't have permission to view ratings for this rental");
            }
        }

        return tenantRatingRepository.findByRentalIdOrderByCreatedAtDesc(rentalId).stream()
                .map(TenantRatingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TenantRatingResponse> findMyRatings() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return tenantRatingRepository.findByRaterIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(TenantRatingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantRatingResponse findById(UUID ratingId) {
        TenantRating rating = getRatingAndCheckAccess(ratingId);
        return TenantRatingResponse.from(rating);
    }

    @Transactional
    public TenantRatingResponse create(CreateTenantRatingRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = getCurrentUser(currentUserId);
        log.info("User {} creating rating for tenant {}", currentUserId, request.tenantId());

        User tenant = userRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.tenantId().toString()));

        if (tenant.getRole() != Role.TENANT) {
            throw new BusinessException("The rated user must be a tenant");
        }

        Rental rental = null;
        if (request.rentalId() != null) {
            rental = rentalRepository.findById(request.rentalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));

            // Verify tenant is linked to this rental
            if (rental.getTenant() == null || !rental.getTenant().getId().equals(request.tenantId())) {
                throw new BusinessException("This tenant is not linked to this rental");
            }

            // OWNER: verify they own the property of this rental
            if (currentUser.getRole() == Role.OWNER && !rental.getProperty().getOwner().getId().equals(currentUserId)) {
                throw new ForbiddenException("You don't have permission to rate tenants of this rental");
            }
        } else if (currentUser.getRole() == Role.OWNER) {
            // OWNER must rate through a rental they own
            validateOwnerAccessToTenant(currentUserId, request.tenantId());
        }

        TenantRating rating = TenantRating.builder()
                .tenant(tenant)
                .rater(currentUser)
                .rental(rental)
                .paymentRating(request.paymentRating())
                .propertyRespectRating(request.propertyRespectRating())
                .communicationRating(request.communicationRating())
                .comment(request.comment())
                .ratingPeriod(request.ratingPeriod())
                .build();

        rating = tenantRatingRepository.save(rating);
        log.info("Rating {} created for tenant {}", rating.getId(), request.tenantId());

        return TenantRatingResponse.from(rating);
    }

    @Transactional
    public TenantRatingResponse update(UUID ratingId, UpdateTenantRatingRequest request) {
        TenantRating rating = getRatingAndCheckAccess(ratingId);
        UUID currentUserId = AuthService.getCurrentUserId();

        // Only the rater or ADMIN can update
        if (!rating.getRater().getId().equals(currentUserId)) {
            User currentUser = getCurrentUser(currentUserId);
            if (currentUser.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You can only update your own ratings");
            }
        }

        log.info("Updating rating {}", ratingId);

        if (request.paymentRating() != null) {
            rating.setPaymentRating(request.paymentRating());
        }
        if (request.propertyRespectRating() != null) {
            rating.setPropertyRespectRating(request.propertyRespectRating());
        }
        if (request.communicationRating() != null) {
            rating.setCommunicationRating(request.communicationRating());
        }
        if (request.comment() != null) {
            rating.setComment(request.comment());
        }
        if (request.ratingPeriod() != null) {
            rating.setRatingPeriod(request.ratingPeriod());
        }

        rating = tenantRatingRepository.saveAndFlush(rating);
        log.info("Rating {} updated successfully", ratingId);

        return TenantRatingResponse.from(rating);
    }

    @Transactional
    public void delete(UUID ratingId) {
        TenantRating rating = getRatingAndCheckAccess(ratingId);
        UUID currentUserId = AuthService.getCurrentUserId();

        // Only the rater or ADMIN can delete
        if (!rating.getRater().getId().equals(currentUserId)) {
            User currentUser = getCurrentUser(currentUserId);
            if (currentUser.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You can only delete your own ratings");
            }
        }

        log.info("Deleting rating {}", ratingId);
        tenantRatingRepository.delete(rating);
        log.info("Rating {} deleted successfully", ratingId);
    }

    private TenantRating getRatingAndCheckAccess(UUID ratingId) {
        TenantRating rating = tenantRatingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRating", "id", ratingId.toString()));

        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = getCurrentUser(currentUserId);

        if (currentUser.getRole() == Role.OWNER) {
            // OWNER can only access ratings they created or for their tenants
            boolean isRater = rating.getRater().getId().equals(currentUserId);
            boolean isPropertyOwner = rating.getRental() != null &&
                    rating.getRental().getProperty().getOwner().getId().equals(currentUserId);

            if (!isRater && !isPropertyOwner) {
                throw new ForbiddenException("You don't have permission to access this rating");
            }
        }

        return rating;
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private void validateOwnerAccessToTenant(UUID ownerId, UUID tenantId) {
        List<Rental> ownerRentals = rentalRepository.findByOwnerId(ownerId);
        boolean hasTenant = ownerRentals.stream()
                .anyMatch(r -> r.getTenant() != null && r.getTenant().getId().equals(tenantId));

        if (!hasTenant) {
            throw new ForbiddenException("You don't have permission to access this tenant's ratings");
        }
    }
}
