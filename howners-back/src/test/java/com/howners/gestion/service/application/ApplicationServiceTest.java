package com.howners.gestion.service.application;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.application.ApplicationResponse;
import com.howners.gestion.dto.application.CreateApplicationRequest;
import com.howners.gestion.dto.application.ReviewApplicationRequest;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.document.DocumentService;
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
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentService documentService;

    @InjectMocks
    private ApplicationService applicationService;

    private UUID tenantId;
    private UUID ownerId;
    private User tenant;
    private User owner;
    private Property property;
    private Listing listing;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        tenant = User.builder()
                .id(tenantId)
                .email("tenant@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .passwordHash("hash")
                .role(Role.TENANT)
                .enabled(true)
                .build();

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
                .name("Appartement Paris 11")
                .owner(owner)
                .build();

        listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Studio meublé")
                .property(property)
                .status(ListingStatus.PUBLISHED)
                .build();

        setCurrentUser(tenant);
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

    // --- submit ---

    @Test
    void submit_shouldCreateApplicationAndReturnEmptyDocuments() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                listing.getId(), "Je suis intéressé", null);

        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(applicationRepository.existsByListingIdAndApplicantId(listing.getId(), tenantId)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(UUID.randomUUID());
            app.setCreatedAt(LocalDateTime.now());
            return app;
        });

        ApplicationResponse response = applicationService.submit(request);

        assertThat(response).isNotNull();
        assertThat(response.applicantId()).isEqualTo(tenantId);
        assertThat(response.applicantName()).contains("Jean");
        assertThat(response.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(response.documents()).isEmpty();
    }

    @Test
    void submit_duplicateApplication_shouldThrow() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                listing.getId(), "Je suis intéressé", null);

        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(applicationRepository.existsByListingIdAndApplicantId(listing.getId(), tenantId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.submit(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    void submit_unpublishedListing_shouldThrow() {
        listing.setStatus(ListingStatus.DRAFT);
        CreateApplicationRequest request = new CreateApplicationRequest(
                listing.getId(), "Test", null);

        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> applicationService.submit(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not published");
    }

    // --- findMyApplications ---

    @Test
    void findMyApplications_shouldIncludeDocuments() {
        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .fileName("id.pdf")
                .fileKey("key1")
                .filePath("key1")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(tenant)
                .uploadedAt(LocalDateTime.now())
                .build();

        DocumentResponse docResponse = new DocumentResponse(
                doc.getId(), "id.pdf", "https://url", 1000L, "application/pdf",
                DocumentType.IDENTITY, null, null, appId, tenantId, "Jean Dupont",
                "hash", null, LocalDateTime.now(), null, null, false, false);

        when(applicationRepository.findByApplicantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of(doc));
        when(documentService.toResponse(doc)).thenReturn(docResponse);

        List<ApplicationResponse> result = applicationService.findMyApplications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).documents()).hasSize(1);
        assertThat(result.get(0).documents().get(0).documentType()).isEqualTo(DocumentType.IDENTITY);
        assertThat(result.get(0).applicantId()).isEqualTo(tenantId);
    }

    @Test
    void findMyApplications_emptyList_shouldReturnEmpty() {
        when(applicationRepository.findByApplicantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of());

        List<ApplicationResponse> result = applicationService.findMyApplications();

        assertThat(result).isEmpty();
    }

    // --- findReceivedApplications ---

    @Test
    void findReceivedApplications_shouldIncludeDocuments() {
        setCurrentUser(owner);

        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(applicationRepository.findByOwnerId(ownerId)).thenReturn(List.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of());

        List<ApplicationResponse> result = applicationService.findReceivedApplications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).documents()).isEmpty();
        assertThat(result.get(0).applicantName()).contains("Jean");
    }

    // --- findById ---

    @Test
    void findById_shouldIncludeDocuments() {
        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of());

        ApplicationResponse result = applicationService.findById(appId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(appId);
        assertThat(result.documents()).isEmpty();
    }

    @Test
    void findById_notFound_shouldThrow() {
        UUID appId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.findById(appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- review ---

    @Test
    void review_shouldUpdateStatusAndIncludeDocuments() {
        setCurrentUser(owner);

        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of());

        ReviewApplicationRequest request = new ReviewApplicationRequest(
                ApplicationStatus.ACCEPTED, "Bon dossier");

        ApplicationResponse result = applicationService.review(appId, request);

        assertThat(result.status()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(result.notes()).isEqualTo("Bon dossier");
        assertThat(result.documents()).isEmpty();
    }

    @Test
    void review_alreadyRejected_shouldThrow() {
        setCurrentUser(owner);

        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.REJECTED) // Already reviewed
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        ReviewApplicationRequest request = new ReviewApplicationRequest(
                ApplicationStatus.ACCEPTED, null);

        assertThatThrownBy(() -> applicationService.review(appId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be reviewed");
    }

    // --- withdraw ---

    @Test
    void withdraw_shouldUpdateStatusAndIncludeDocuments() {
        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of());

        ApplicationResponse result = applicationService.withdraw(appId);

        assertThat(result.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(result.documents()).isEmpty();
    }

    @Test
    void withdraw_notApplicant_shouldThrow() {
        setCurrentUser(owner); // Not the applicant

        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant) // Tenant is the applicant
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.withdraw(appId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own applications");
    }

    // --- Response structure ---

    @Test
    void applicationResponse_shouldContainAllExpectedFields() {
        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .listing(listing)
                .applicant(tenant)
                .coverLetter("Ma lettre")
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        Document doc1 = Document.builder()
                .id(UUID.randomUUID())
                .fileName("id.pdf")
                .fileKey("k1")
                .filePath("k1")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(tenant)
                .uploadedAt(LocalDateTime.now())
                .build();

        Document doc2 = Document.builder()
                .id(UUID.randomUUID())
                .fileName("pay1.pdf")
                .fileKey("k2")
                .filePath("k2")
                .documentType(DocumentType.PROOF_OF_INCOME)
                .application(application)
                .uploader(tenant)
                .uploadedAt(LocalDateTime.now())
                .build();

        DocumentResponse dr1 = new DocumentResponse(
                doc1.getId(), "id.pdf", "https://url1", 100L, "application/pdf",
                DocumentType.IDENTITY, null, null, appId, tenantId, "Jean Dupont",
                "h1", null, LocalDateTime.now(), null, null, false, false);
        DocumentResponse dr2 = new DocumentResponse(
                doc2.getId(), "pay1.pdf", "https://url2", 200L, "application/pdf",
                DocumentType.PROOF_OF_INCOME, null, null, appId, tenantId, "Jean Dupont",
                "h2", null, LocalDateTime.now(), null, null, false, false);

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of(doc1, doc2));
        when(documentService.toResponse(doc1)).thenReturn(dr1);
        when(documentService.toResponse(doc2)).thenReturn(dr2);

        ApplicationResponse result = applicationService.findById(appId);

        assertThat(result.id()).isEqualTo(appId);
        assertThat(result.listingId()).isEqualTo(listing.getId());
        assertThat(result.listingTitle()).isEqualTo("Studio meublé");
        assertThat(result.propertyName()).isEqualTo("Appartement Paris 11");
        assertThat(result.applicantId()).isEqualTo(tenantId);
        assertThat(result.applicantName()).contains("Jean");
        assertThat(result.applicantEmail()).isEqualTo("tenant@test.com");
        assertThat(result.coverLetter()).isEqualTo("Ma lettre");
        assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(result.documents()).hasSize(2);
        assertThat(result.documents().get(0).documentType()).isEqualTo(DocumentType.IDENTITY);
        assertThat(result.documents().get(1).documentType()).isEqualTo(DocumentType.PROOF_OF_INCOME);
    }
}
