package com.howners.gestion.service.rating;

import com.howners.gestion.domain.rating.OwnerRating;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rating.CreateOwnerRatingRequest;
import com.howners.gestion.dto.rating.OwnerRatingResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.OwnerRatingRepository;
import com.howners.gestion.repository.RentalRepository;
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
public class OwnerRatingService {

    private final OwnerRatingRepository ratingRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;

    @Transactional
    public OwnerRatingResponse create(CreateOwnerRatingRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        User rater = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        // Vérifier que le locataire a bien une location avec ce propriétaire
        List<Rental> sharedRentals = rentalRepository.findByOwnerIdAndTenantId(request.ownerId(), currentUserId);
        if (sharedRentals.isEmpty()) {
            throw new ForbiddenException("Vous ne pouvez noter que vos propriétaires actuels ou passés");
        }

        Rental rental = null;
        if (request.rentalId() != null) {
            rental = sharedRentals.stream()
                    .filter(r -> r.getId().equals(request.rentalId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Cette location ne vous concerne pas"));

            if (ratingRepository.existsByRaterIdAndRentalId(currentUserId, request.rentalId())) {
                throw new BadRequestException("Vous avez déjà laissé un avis pour cette location");
            }
        }

        BigDecimal overall = BigDecimal.valueOf(
                (request.communicationRating() + request.responsivenessRating() + request.contractRespectRating()) / 3.0
        ).setScale(2, RoundingMode.HALF_UP);

        OwnerRating rating = OwnerRating.builder()
                .owner(owner)
                .rater(rater)
                .rental(rental)
                .communicationRating(request.communicationRating())
                .responsivenessRating(request.responsivenessRating())
                .contractRespectRating(request.contractRespectRating())
                .overallRating(overall)
                .comment(request.comment())
                .build();

        rating = ratingRepository.save(rating);
        log.info("Owner rating created by {} for owner {}", currentUserId, request.ownerId());
        return OwnerRatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<OwnerRatingResponse> getRatingsForOwner(UUID ownerId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        // Accessible au proprio lui-même ou à un locataire qui a loué chez lui
        boolean isSelf = currentUserId.equals(ownerId);
        boolean isTenantWithOwner = !rentalRepository.findByOwnerIdAndTenantId(ownerId, currentUserId).isEmpty();

        if (!isSelf && !isTenantWithOwner) {
            throw new ForbiddenException("Accès non autorisé");
        }

        return ratingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(OwnerRatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OwnerRatingResponse> getMyRatings() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return ratingRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserId)
                .stream().map(OwnerRatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getOwnerProfile(UUID ownerId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        List<Rental> shared = rentalRepository.findByOwnerIdAndTenantId(ownerId, currentUserId);
        if (shared.isEmpty()) {
            throw new ForbiddenException("Vous ne pouvez consulter que vos propriétaires");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        return UserResponse.from(owner);
    }
}
