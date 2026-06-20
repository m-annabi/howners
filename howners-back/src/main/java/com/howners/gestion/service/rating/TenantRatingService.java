package com.howners.gestion.service.rating;

import com.howners.gestion.domain.rating.TenantRating;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rating.CreateTenantRatingRequest;
import com.howners.gestion.dto.rating.TenantRatingResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.exception.BadRequestException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRatingService {

    private final TenantRatingRepository ratingRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;

    @Transactional
    public TenantRatingResponse create(CreateTenantRatingRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        User rater = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User tenant = userRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Vérifier que le proprio a bien une location avec ce locataire
        List<Rental> sharedRentals = rentalRepository.findByOwnerIdAndTenantId(currentUserId, request.tenantId());
        if (sharedRentals.isEmpty()) {
            throw new ForbiddenException("Vous ne pouvez noter que vos locataires actuels ou passés");
        }

        Rental rental = null;
        if (request.rentalId() != null) {
            rental = sharedRentals.stream()
                    .filter(r -> r.getId().equals(request.rentalId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Cette location ne vous appartient pas"));

            if (ratingRepository.existsByRaterIdAndRentalId(currentUserId, request.rentalId())) {
                throw new BadRequestException("Vous avez déjà laissé un avis pour cette location");
            }
        }

        BigDecimal overall = BigDecimal.valueOf(
                (request.paymentRating() + request.propertyRespectRating() + request.communicationRating()) / 3.0
        ).setScale(2, RoundingMode.HALF_UP);

        TenantRating rating = TenantRating.builder()
                .tenant(tenant)
                .rater(rater)
                .rental(rental)
                .paymentRating(request.paymentRating())
                .propertyRespectRating(request.propertyRespectRating())
                .communicationRating(request.communicationRating())
                .overallRating(overall)
                .comment(request.comment())
                .build();

        rating = ratingRepository.save(rating);
        log.info("Tenant rating created by {} for tenant {}", currentUserId, request.tenantId());
        return TenantRatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<TenantRatingResponse> getRatingsForTenant(UUID tenantId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        // Accessible au locataire lui-même ou à un proprio qui a eu ce locataire
        boolean isSelf = currentUserId.equals(tenantId);
        boolean isOwnerWithTenant = !rentalRepository.findByOwnerIdAndTenantId(currentUserId, tenantId).isEmpty();

        if (!isSelf && !isOwnerWithTenant) {
            throw new ForbiddenException("Accès non autorisé");
        }

        return ratingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(TenantRatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TenantRatingResponse> getMyRatings() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return ratingRepository.findByTenantIdOrderByCreatedAtDesc(currentUserId)
                .stream().map(TenantRatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getTenantProfile(UUID tenantId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        List<Rental> shared = rentalRepository.findByOwnerIdAndTenantId(currentUserId, tenantId);
        if (shared.isEmpty()) {
            throw new ForbiddenException("Vous ne pouvez consulter que vos locataires");
        }

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return UserResponse.from(tenant);
    }
}
