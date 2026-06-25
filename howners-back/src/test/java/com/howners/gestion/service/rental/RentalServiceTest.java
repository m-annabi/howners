package com.howners.gestion.service.rental;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.listing.ListingResponse;
import com.howners.gestion.dto.request.CreateRentalRequest;
import com.howners.gestion.dto.request.ExitTenantRequest;
import com.howners.gestion.dto.request.PublishRentalRequest;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.subscription.FeatureGateService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock private RentalRepository rentalRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private ContractRepository contractRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private FeatureGateService featureGateService;
    @Mock private ListingRepository listingRepository;

    @InjectMocks
    private RentalService rentalService;

    private UUID ownerId;
    private UUID tenantId;
    private User owner;
    private User tenant;
    private Property property;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        owner = User.builder().id(ownerId).email("owner@test.com")
                .firstName("Marie").lastName("Martin").passwordHash("h").role(Role.OWNER).enabled(true).build();
        tenant = User.builder().id(tenantId).email("tenant@test.com")
                .firstName("Jean").lastName("Dupont").passwordHash("h").role(Role.TENANT).enabled(true).build();
        property = Property.builder().id(UUID.randomUUID()).name("Appartement Paris 11").owner(owner).build();
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

    private Rental rental(RentalStatus status) {
        return Rental.builder().id(UUID.randomUUID()).property(property).status(status)
                .monthlyRent(new BigDecimal("1200")).currency("EUR").build();
    }

    private void stubSaveReturnsArg() {
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- accès ---

    @Test
    void findById_asOwner_returns() {
        Rental r = rental(RentalStatus.ACTIVE);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        RentalResponse result = rentalService.findById(r.getId());

        assertThat(result.id()).isEqualTo(r.getId());
        assertThat(result.status()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void findById_asTenant_returns() {
        Rental r = rental(RentalStatus.ACTIVE);
        r.setTenant(tenant);
        setCurrentUser(tenant);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThat(rentalService.findById(r.getId()).tenantId()).isEqualTo(tenantId);
    }

    @Test
    void findById_unauthorized_throws() {
        Rental r = rental(RentalStatus.ACTIVE);
        r.setTenant(tenant);
        User intruder = User.builder().id(UUID.randomUUID()).email("x@test.com")
                .firstName("In").lastName("Trus").passwordHash("h").role(Role.OWNER).enabled(true).build();
        setCurrentUser(intruder);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.findById(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(rentalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- create ---

    @Test
    void create_asOwner_setsVacantAndAudits() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        stubSaveReturnsArg();
        CreateRentalRequest req = new CreateRentalRequest(property.getId(), LocalDate.now(), null,
                new BigDecimal("1200"), "EUR", new BigDecimal("1200"), new BigDecimal("50"), 5, null);

        RentalResponse result = rentalService.create(req);

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalStatus.VACANT);
        assertThat(result.status()).isEqualTo(RentalStatus.VACANT);
        verify(auditService).logAction(any(), eq("Rental"), any());
    }

    @Test
    void create_notOwnerOfProperty_throwsBusiness() {
        User otherOwner = User.builder().id(UUID.randomUUID()).email("o2@test.com")
                .firstName("Paul").lastName("Petit").passwordHash("h").role(Role.OWNER).enabled(true).build();
        Property notMine = Property.builder().id(UUID.randomUUID()).name("Autre").owner(otherOwner).build();
        when(propertyRepository.findById(notMine.getId())).thenReturn(Optional.of(notMine));
        CreateRentalRequest req = new CreateRentalRequest(notMine.getId(), LocalDate.now(), null,
                new BigDecimal("1000"), "EUR", null, null, 1, null);

        assertThatThrownBy(() -> rentalService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void create_propertyNotFound_throwsNotFound() {
        UUID pid = UUID.randomUUID();
        when(propertyRepository.findById(pid)).thenReturn(Optional.empty());
        CreateRentalRequest req = new CreateRentalRequest(pid, LocalDate.now(), null,
                new BigDecimal("1000"), "EUR", null, null, 1, null);

        assertThatThrownBy(() -> rentalService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- publish (VACANT -> LISTED) ---

    @Test
    void publish_vacant_createsListingAndSetsListed() {
        Rental r = rental(RentalStatus.VACANT);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(listingRepository.findByRentalId(r.getId())).thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSaveReturnsArg();
        PublishRentalRequest req = new PublishRentalRequest("Studio lumineux", "Proche métro", LocalDate.now());

        ListingResponse result = rentalService.publish(r.getId(), req);

        ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        assertThat(rentalCaptor.getValue().getStatus()).isEqualTo(RentalStatus.LISTED);
        ArgumentCaptor<Listing> listingCaptor = ArgumentCaptor.forClass(Listing.class);
        verify(listingRepository).save(listingCaptor.capture());
        assertThat(listingCaptor.getValue().getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(result.title()).isEqualTo("Studio lumineux");
    }

    @Test
    void publish_notVacant_throwsBadRequest() {
        Rental r = rental(RentalStatus.ACTIVE);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        PublishRentalRequest req = new PublishRentalRequest("X", null, LocalDate.now());

        assertThatThrownBy(() -> rentalService.publish(r.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VACANT");
        verify(listingRepository, never()).save(any());
    }

    // --- exitTenant (ACTIVE -> EXITING) ---

    @Test
    void exitTenant_active_setsExitingAndPublishesListing() {
        Rental r = rental(RentalStatus.ACTIVE);
        r.setTenant(tenant);
        LocalDate exit = LocalDate.now().plusMonths(2);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(listingRepository.findByRentalId(r.getId())).thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSaveReturnsArg();

        RentalResponse result = rentalService.exitTenant(r.getId(), new ExitTenantRequest(exit, "Préavis reçu"));

        assertThat(result.status()).isEqualTo(RentalStatus.EXITING);
        assertThat(r.getEndDate()).isEqualTo(exit);
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void exitTenant_notActive_throwsBadRequest() {
        Rental r = rental(RentalStatus.VACANT);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.exitTenant(r.getId(), new ExitTenantRequest(LocalDate.now(), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void exitTenant_noTenant_throwsBadRequest() {
        Rental r = rental(RentalStatus.ACTIVE); // pas de tenant
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.exitTenant(r.getId(), new ExitTenantRequest(LocalDate.now(), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No tenant");
    }

    // --- confirmExit (EXITING -> ACTIVE/VACANT) ---

    @Test
    void confirmExit_withAcceptedApplication_activatesNextTenant() {
        User next = User.builder().id(UUID.randomUUID()).email("next@test.com")
                .firstName("Lea").lastName("Moreau").passwordHash("h").role(Role.TENANT).enabled(true).build();
        Application app = Application.builder().id(UUID.randomUUID())
                .applicant(next).status(ApplicationStatus.ACCEPTED).build();
        Rental r = rental(RentalStatus.EXITING);
        r.setTenant(tenant);
        LocalDate end = LocalDate.now().plusMonths(1);
        r.setEndDate(end);
        r.setApplication(app);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        stubSaveReturnsArg();

        RentalResponse result = rentalService.confirmExit(r.getId());

        assertThat(result.status()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(r.getTenant()).isEqualTo(next);
        assertThat(r.getStartDate()).isEqualTo(end);
        assertThat(r.getEndDate()).isNull();
    }

    @Test
    void confirmExit_withoutNextTenant_becomesVacant() {
        Rental r = rental(RentalStatus.EXITING);
        r.setTenant(tenant);
        r.setEndDate(LocalDate.now());
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        stubSaveReturnsArg();

        RentalResponse result = rentalService.confirmExit(r.getId());

        assertThat(result.status()).isEqualTo(RentalStatus.VACANT);
        assertThat(r.getTenant()).isNull();
    }

    @Test
    void confirmExit_notExiting_throwsBadRequest() {
        Rental r = rental(RentalStatus.ACTIVE);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.confirmExit(r.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("EXITING");
    }

    // --- delete ---

    @Test
    void delete_withContracts_throwsBusiness() {
        Rental r = rental(RentalStatus.VACANT);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(List.of(mock(Contract.class)));

        assertThatThrownBy(() -> rentalService.delete(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contract");
        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void delete_withPendingPayments_throwsBusiness() {
        Rental r = rental(RentalStatus.VACANT);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(List.of());
        when(paymentRepository.findByRentalIdAndStatus(r.getId(), PaymentStatus.PENDING))
                .thenReturn(List.of(mock(Payment.class)));

        assertThatThrownBy(() -> rentalService.delete(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");
        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void delete_clean_deletesRental() {
        Rental r = rental(RentalStatus.VACANT);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(List.of());
        when(paymentRepository.findByRentalIdAndStatus(r.getId(), PaymentStatus.PENDING)).thenReturn(List.of());

        rentalService.delete(r.getId());

        verify(rentalRepository).delete(r);
    }

    // --- routing par rôle ---

    @Test
    void findAllByCurrentUser_owner_usesOwnerQuery() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(rentalRepository.findByOwnerId(ownerId)).thenReturn(List.of(rental(RentalStatus.ACTIVE)));

        List<RentalResponse> result = rentalService.findAllByCurrentUser();

        assertThat(result).hasSize(1);
        verify(rentalRepository).findByOwnerId(ownerId);
        verify(rentalRepository, never()).findByTenantId(any(UUID.class));
    }
}
