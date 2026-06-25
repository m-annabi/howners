package com.howners.gestion.service.rating;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rating.TenantRating;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
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
import com.howners.gestion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantRatingServiceTest {

    @Mock private TenantRatingRepository ratingRepository;
    @Mock private RentalRepository rentalRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TenantRatingService ratingService;

    private UUID ownerId;
    private UUID tenantId;
    private User owner;
    private User tenant;
    private Property property;
    private Rental rental;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        owner = User.builder().id(ownerId).email("owner@test.com")
                .firstName("Marie").lastName("Martin").passwordHash("h").role(Role.OWNER).enabled(true).build();
        tenant = User.builder().id(tenantId).email("tenant@test.com")
                .firstName("Jean").lastName("Dupont").passwordHash("h").role(Role.TENANT).enabled(true).build();

        property = Property.builder().id(UUID.randomUUID()).name("Appartement Paris 11").owner(owner).build();
        rental = Rental.builder().id(UUID.randomUUID()).property(property).tenant(tenant).build();

        setCurrentUser(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentUser(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CreateTenantRatingRequest request(UUID rentalId, int pay, int respect, int comm) {
        return new CreateTenantRatingRequest(tenantId, rentalId, pay, respect, comm, "RAS");
    }

    // --- create ---

    @Test
    void create_withRental_computesOverallHalfUpAndSaves() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of(rental));
        when(ratingRepository.existsByRaterIdAndRentalId(ownerId, rental.getId())).thenReturn(false);
        when(ratingRepository.save(any(TenantRating.class))).thenAnswer(inv -> {
            TenantRating r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        // (5 + 4 + 4) / 3 = 4.3333... -> 4.33 (HALF_UP, 2 décimales)
        TenantRatingResponse response = ratingService.create(request(rental.getId(), 5, 4, 4));

        ArgumentCaptor<TenantRating> captor = ArgumentCaptor.forClass(TenantRating.class);
        verify(ratingRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallRating()).isEqualByComparingTo(new BigDecimal("4.33"));
        assertThat(captor.getValue().getTenant()).isEqualTo(tenant);
        assertThat(captor.getValue().getRater()).isEqualTo(owner);
        assertThat(captor.getValue().getRental()).isEqualTo(rental);
        assertThat(response.overallRating()).isEqualByComparingTo(new BigDecimal("4.33"));
        assertThat(response.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void create_withoutRental_skipsDuplicateCheckAndSaves() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of(rental));
        when(ratingRepository.save(any(TenantRating.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantRatingResponse response = ratingService.create(request(null, 3, 3, 3));

        ArgumentCaptor<TenantRating> captor = ArgumentCaptor.forClass(TenantRating.class);
        verify(ratingRepository).save(captor.capture());
        assertThat(captor.getValue().getRental()).isNull();
        assertThat(captor.getValue().getOverallRating()).isEqualByComparingTo(new BigDecimal("3.00"));
        // Pas de location -> pas de vérification de doublon
        verify(ratingRepository, never()).existsByRaterIdAndRentalId(any(), any());
        assertThat(response.rentalId()).isNull();
    }

    @Test
    void create_noSharedRental_throwsForbidden() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of());

        assertThatThrownBy(() -> ratingService.create(request(null, 4, 4, 4)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("locataires");
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void create_rentalNotOwnedByRater_throwsBadRequest() {
        UUID otherRentalId = UUID.randomUUID();
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of(rental));

        assertThatThrownBy(() -> ratingService.create(request(otherRentalId, 4, 4, 4)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("appartient");
    }

    @Test
    void create_duplicateRatingForRental_throwsBadRequest() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of(rental));
        when(ratingRepository.existsByRaterIdAndRentalId(ownerId, rental.getId())).thenReturn(true);

        assertThatThrownBy(() -> ratingService.create(request(rental.getId(), 4, 4, 4)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà");
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void create_raterNotFound_throwsNotFound() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.create(request(null, 4, 4, 4)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getRatingsForTenant ---

    @Test
    void getRatingsForTenant_self_returnsRatings() {
        setCurrentUser(tenant);
        TenantRating rating = TenantRating.builder().id(UUID.randomUUID())
                .tenant(tenant).rater(owner).overallRating(new BigDecimal("4.50")).build();
        when(ratingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(rating));

        List<TenantRatingResponse> result = ratingService.getRatingsForTenant(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).raterName()).contains("Marie");
    }

    @Test
    void getRatingsForTenant_unauthorized_throwsForbidden() {
        User intruder = User.builder().id(UUID.randomUUID()).email("x@test.com")
                .firstName("In").lastName("Trus").passwordHash("h").role(Role.OWNER).enabled(true).build();
        setCurrentUser(intruder);
        when(rentalRepository.findByOwnerIdAndTenantId(intruder.getId(), tenantId)).thenReturn(List.of());

        assertThatThrownBy(() -> ratingService.getRatingsForTenant(tenantId))
                .isInstanceOf(ForbiddenException.class);
        verify(ratingRepository, never()).findByTenantIdOrderByCreatedAtDesc(any());
    }

    // --- getTenantProfile ---

    @Test
    void getTenantProfile_owner_returnsProfile() {
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of(rental));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserResponse result = ratingService.getTenantProfile(tenantId);

        assertThat(result.email()).isEqualTo("tenant@test.com");
    }

    @Test
    void getTenantProfile_notOwner_throwsForbidden() {
        when(rentalRepository.findByOwnerIdAndTenantId(ownerId, tenantId)).thenReturn(List.of());

        assertThatThrownBy(() -> ratingService.getTenantProfile(tenantId))
                .isInstanceOf(ForbiddenException.class);
    }
}
