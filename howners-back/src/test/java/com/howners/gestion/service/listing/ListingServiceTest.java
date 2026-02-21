package com.howners.gestion.service.listing;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.listing.CreateListingRequest;
import com.howners.gestion.dto.listing.ListingResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.photo.ListingPhotoService;
import com.howners.gestion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private ListingPhotoService listingPhotoService;

    @InjectMocks
    private ListingService listingService;

    private UUID ownerId;
    private User owner;
    private Property property;
    private Listing publishedListing;

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

        property = Property.builder()
                .id(UUID.randomUUID())
                .name("Appartement Suresnes")
                .owner(owner)
                .propertyType(PropertyType.APARTMENT)
                .city("Suresnes")
                .postalCode("92150")
                .department("92")
                .country("FR")
                .build();

        publishedListing = Listing.builder()
                .id(UUID.randomUUID())
                .property(property)
                .title("Studio meuble Suresnes")
                .description("Beau studio proche tramway")
                .pricePerMonth(new BigDecimal("850"))
                .currency("EUR")
                .status(ListingStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
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

    // --- searchPublished ---

    @Test
    void searchPublished_withSearchTerm_shouldReturnMatchingListings() {
        when(listingRepository.searchPublished("Suresnes")).thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublished("Suresnes");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).propertyCity()).isEqualTo("Suresnes");
        assertThat(result.get(0).propertyPostalCode()).isEqualTo("92150");
        assertThat(result.get(0).propertyDepartment()).isEqualTo("92");
        assertThat(result.get(0).propertyCountry()).isEqualTo("FR");
    }

    @Test
    void searchPublished_withNullSearch_shouldReturnAllPublished() {
        when(listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublished(null);

        assertThat(result).hasSize(1);
        verify(listingRepository, never()).searchPublished(any());
    }

    @Test
    void searchPublished_withBlankSearch_shouldReturnAllPublished() {
        when(listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED))
                .thenReturn(List.of());

        List<ListingResponse> result = listingService.searchPublished("   ");

        assertThat(result).isEmpty();
        verify(listingRepository, never()).searchPublished(any());
    }

    // --- searchPublishedWithFilters ---

    @Test
    void searchWithFilters_byCity_shouldFilterCorrectly() {
        when(listingRepository.searchPublishedWithFilters("", "Suresnes", "", ""))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters(null, "Suresnes", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).propertyCity()).isEqualTo("Suresnes");
    }

    @Test
    void searchWithFilters_byDepartment_shouldFilterCorrectly() {
        when(listingRepository.searchPublishedWithFilters("", "", "92", ""))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters(null, null, "92", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).propertyDepartment()).isEqualTo("92");
    }

    @Test
    void searchWithFilters_byPostalCodePrefix_shouldFilterCorrectly() {
        when(listingRepository.searchPublishedWithFilters("", "", "", "92"))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters(null, null, null, "92");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchWithFilters_combinedSearchAndDepartment_shouldWork() {
        when(listingRepository.searchPublishedWithFilters("studio", "", "92", ""))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters("studio", null, "92", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchWithFilters_allNull_shouldReturnAllPublished() {
        when(listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters(null, null, null, null);

        assertThat(result).hasSize(1);
        verify(listingRepository, never()).searchPublishedWithFilters(any(), any(), any(), any());
    }

    @Test
    void searchWithFilters_allBlank_shouldReturnAllPublished() {
        when(listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED))
                .thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.searchPublishedWithFilters("", "", "", "");

        assertThat(result).hasSize(1);
        verify(listingRepository, never()).searchPublishedWithFilters(any(), any(), any(), any());
    }

    // --- findById ---

    @Test
    void findById_shouldReturnListingWithGeoFields() {
        when(listingRepository.findById(publishedListing.getId())).thenReturn(Optional.of(publishedListing));

        ListingResponse result = listingService.findById(publishedListing.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(publishedListing.getId());
        assertThat(result.propertyCity()).isEqualTo("Suresnes");
        assertThat(result.propertyPostalCode()).isEqualTo("92150");
        assertThat(result.propertyDepartment()).isEqualTo("92");
        assertThat(result.propertyCountry()).isEqualTo("FR");
        assertThat(result.ownerName()).contains("Marie");
    }

    @Test
    void findById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(listingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- create ---

    @Test
    void create_shouldReturnDraftListing() {
        CreateListingRequest request = new CreateListingRequest(
                property.getId(), "Nouveau studio", "Description",
                null, new BigDecimal("900"), null, null, null, null, null, null);

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            l.setCreatedAt(LocalDateTime.now());
            return l;
        });

        ListingResponse result = listingService.create(request);

        assertThat(result.status()).isEqualTo(ListingStatus.DRAFT);
        assertThat(result.title()).isEqualTo("Nouveau studio");
        assertThat(result.propertyCity()).isEqualTo("Suresnes");
        assertThat(result.propertyDepartment()).isEqualTo("92");
    }

    // --- publish ---

    @Test
    void publish_draftListing_shouldSetPublishedStatus() {
        Listing draft = Listing.builder()
                .id(UUID.randomUUID())
                .property(property)
                .title("Draft listing")
                .status(ListingStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        when(listingRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse result = listingService.publish(draft.getId());

        assertThat(result.status()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(result.publishedAt()).isNotNull();
    }

    @Test
    void publish_closedListing_shouldThrow() {
        Listing closed = Listing.builder()
                .id(UUID.randomUUID())
                .property(property)
                .title("Closed listing")
                .status(ListingStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build();

        when(listingRepository.findById(closed.getId())).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> listingService.publish(closed.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT or PAUSED");
    }

    // --- pause ---

    @Test
    void pause_shouldSetPausedStatus() {
        when(listingRepository.findById(publishedListing.getId())).thenReturn(Optional.of(publishedListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse result = listingService.pause(publishedListing.getId());

        assertThat(result.status()).isEqualTo(ListingStatus.PAUSED);
    }

    // --- close ---

    @Test
    void close_shouldSetClosedStatus() {
        when(listingRepository.findById(publishedListing.getId())).thenReturn(Optional.of(publishedListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse result = listingService.close(publishedListing.getId());

        assertThat(result.status()).isEqualTo(ListingStatus.CLOSED);
    }

    // --- delete ---

    @Test
    void delete_shouldCallRepositoryDelete() {
        when(listingRepository.findById(publishedListing.getId())).thenReturn(Optional.of(publishedListing));

        listingService.delete(publishedListing.getId());

        verify(listingRepository).delete(publishedListing);
    }

    @Test
    void delete_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(listingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- ListingResponse structure ---

    @Test
    void listingResponse_shouldContainAllGeoFields() {
        Property geoProperty = Property.builder()
                .id(UUID.randomUUID())
                .name("Chalet Geneve")
                .owner(owner)
                .propertyType(PropertyType.HOUSE)
                .city("Geneve")
                .postalCode("1201")
                .department("GE")
                .country("CH")
                .build();

        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .property(geoProperty)
                .title("Chalet avec vue")
                .pricePerMonth(new BigDecimal("2500"))
                .currency("CHF")
                .status(ListingStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        ListingResponse result = listingService.findById(listing.getId());

        assertThat(result.propertyName()).isEqualTo("Chalet Geneve");
        assertThat(result.propertyCity()).isEqualTo("Geneve");
        assertThat(result.propertyPostalCode()).isEqualTo("1201");
        assertThat(result.propertyDepartment()).isEqualTo("GE");
        assertThat(result.propertyCountry()).isEqualTo("CH");
        assertThat(result.currency()).isEqualTo("CHF");
    }

    @Test
    void listingResponse_withNullDepartment_shouldHandleGracefully() {
        Property noDeptProperty = Property.builder()
                .id(UUID.randomUUID())
                .name("Old property")
                .owner(owner)
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .build();

        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .property(noDeptProperty)
                .title("Listing sans departement")
                .status(ListingStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .build();

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        ListingResponse result = listingService.findById(listing.getId());

        assertThat(result.propertyDepartment()).isNull();
        assertThat(result.propertyCity()).isEqualTo("Paris");
        assertThat(result.propertyPostalCode()).isEqualTo("75001");
    }

    // --- findMyListings ---

    @Test
    void findMyListings_asOwner_shouldReturnOwnListings() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(listingRepository.findByOwnerId(ownerId)).thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.findMyListings();

        assertThat(result).hasSize(1);
        verify(listingRepository).findByOwnerId(ownerId);
        verify(listingRepository, never()).findAll();
    }

    @Test
    void findMyListings_asAdmin_shouldReturnAllListings() {
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        setCurrentUser(admin);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(listingRepository.findAll()).thenReturn(List.of(publishedListing));

        List<ListingResponse> result = listingService.findMyListings();

        assertThat(result).hasSize(1);
        verify(listingRepository).findAll();
        verify(listingRepository, never()).findByOwnerId(any());
    }
}
