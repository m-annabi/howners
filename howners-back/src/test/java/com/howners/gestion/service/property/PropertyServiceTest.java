package com.howners.gestion.service.property;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.AddressDTO;
import com.howners.gestion.dto.request.CreatePropertyRequest;
import com.howners.gestion.dto.request.UpdatePropertyRequest;
import com.howners.gestion.dto.response.PropertyResponse;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.subscription.FeatureGateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private RentalRepository rentalRepository;
    @Mock private UserRepository userRepository;
    @Mock private FeatureGateService featureGateService;

    @InjectMocks
    private PropertyService propertyService;

    private UUID ownerId;
    private User owner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .firstName("Marie")
                .lastName("Martin")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();

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

    private CreatePropertyRequest createRequest(AddressDTO address, String name) {
        // 22 params: name, propertyType, address, surfaceArea, bedrooms, bathrooms,
        // description, condoFees, propertyTax, businessTax, homeInsurance, purchasePrice,
        // dpeRating, gesRating, constructionYear, floorNumber, totalFloors,
        // heatingType, hasParking, hasElevator, isFurnished, propertyCondition
        return new CreatePropertyRequest(
                name, PropertyType.APARTMENT, address,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    // --- create ---

    @Test
    void create_withDepartment_shouldPersistDepartment() {
        AddressDTO address = new AddressDTO("10 Rue de la Gare", null, "Suresnes", "92150", "92", "FR");
        CreatePropertyRequest request = createRequest(address, "Appartement Suresnes");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PropertyResponse result = propertyService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.address().department()).isEqualTo("92");
        assertThat(result.address().city()).isEqualTo("Suresnes");
        assertThat(result.address().postalCode()).isEqualTo("92150");
        assertThat(result.address().country()).isEqualTo("FR");

        verify(propertyRepository).save(argThat(p ->
                "92".equals(p.getDepartment()) && "Suresnes".equals(p.getCity())
        ));
    }

    @Test
    void create_withSwissAddress_shouldPersistCanton() {
        AddressDTO address = new AddressDTO("Rue du Rhone 1", null, "Geneve", "1201", "GE", "CH");
        CreatePropertyRequest request = new CreatePropertyRequest(
                "Chalet Geneve", PropertyType.HOUSE, address,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PropertyResponse result = propertyService.create(request);

        assertThat(result.address().department()).isEqualTo("GE");
        assertThat(result.address().country()).isEqualTo("CH");
    }

    @Test
    void create_withNullDepartment_shouldWork() {
        AddressDTO address = new AddressDTO("1 Rue Test", null, "Paris", "75001", null, "FR");
        CreatePropertyRequest request = createRequest(address, "Apt Paris");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PropertyResponse result = propertyService.create(request);

        assertThat(result.address().department()).isNull();
        assertThat(result.address().city()).isEqualTo("Paris");
    }

    // --- update ---

    @Test
    void update_department_shouldUpdateDepartment() {
        Property existing = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        AddressDTO newAddress = new AddressDTO(null, null, "Suresnes", "92150", "92", null);
        UpdatePropertyRequest request = new UpdatePropertyRequest(
                null, null, newAddress,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null);

        when(propertyRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponse result = propertyService.update(existing.getId(), request);

        assertThat(result.address().department()).isEqualTo("92");
        assertThat(result.address().city()).isEqualTo("Suresnes");
        assertThat(result.address().postalCode()).isEqualTo("92150");
    }

    // --- findById ---

    @Test
    void findById_shouldReturnPropertyWithDepartment() {
        Property p = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt")
                .propertyType(PropertyType.APARTMENT)
                .city("Suresnes")
                .postalCode("92150")
                .department("92")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

        PropertyResponse result = propertyService.findById(p.getId());

        assertThat(result.address().department()).isEqualTo("92");
    }

    @Test
    void findById_notOwner_shouldThrow() {
        User otherOwner = User.builder()
                .id(UUID.randomUUID())
                .email("other@test.com")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        Property p = Property.builder()
                .id(UUID.randomUUID())
                .owner(otherOwner)
                .name("Not mine")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> propertyService.findById(p.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    // --- findAllByCurrentUser ---

    @Test
    void findAllByCurrentUser_shouldReturnListWithDepartments() {
        Property p1 = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt 1")
                .propertyType(PropertyType.APARTMENT)
                .city("Suresnes")
                .postalCode("92150")
                .department("92")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        Property p2 = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt 2")
                .propertyType(PropertyType.STUDIO)
                .city("Geneve")
                .postalCode("1201")
                .department("GE")
                .country("CH")
                .createdAt(LocalDateTime.now())
                .build();

        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(p1, p2));

        List<PropertyResponse> result = propertyService.findAllByCurrentUser();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).address().department()).isEqualTo("92");
        assertThat(result.get(1).address().department()).isEqualTo("GE");
        assertThat(result.get(1).address().country()).isEqualTo("CH");
    }

    // --- AddressDTO default country ---

    @Test
    void addressDTO_nullCountry_shouldDefaultToFR() {
        AddressDTO address = new AddressDTO("1 Rue", null, "Paris", "75001", "75", null);

        assertThat(address.country()).isEqualTo("FR");
    }

    @Test
    void addressDTO_blankCountry_shouldDefaultToFR() {
        AddressDTO address = new AddressDTO("1 Rue", null, "Paris", "75001", "75", "  ");

        assertThat(address.country()).isEqualTo("FR");
    }

    // --- delete ---

    @Test
    void delete_noActiveRentals_shouldSucceed() {
        Property p = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt to delete")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(rentalRepository.findByPropertyId(p.getId())).thenReturn(List.of());

        propertyService.delete(p.getId());

        verify(propertyRepository).delete(p);
    }

    @Test
    void delete_withActiveRental_shouldThrow() {
        Property p = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt with rental")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        Rental activeRental = Rental.builder()
                .id(UUID.randomUUID())
                .property(p)
                .status(RentalStatus.ACTIVE)
                .build();

        when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(rentalRepository.findByPropertyId(p.getId())).thenReturn(List.of(activeRental));

        assertThatThrownBy(() -> propertyService.delete(p.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete property");

        verify(propertyRepository, never()).delete(any());
    }

    @Test
    void delete_withTerminatedRental_shouldSucceed() {
        Property p = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt with old rental")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        Rental terminatedRental = Rental.builder()
                .id(UUID.randomUUID())
                .property(p)
                .status(RentalStatus.TERMINATED)
                .build();

        when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(rentalRepository.findByPropertyId(p.getId())).thenReturn(List.of(terminatedRental));

        propertyService.delete(p.getId());

        verify(propertyRepository).delete(p);
    }
}
