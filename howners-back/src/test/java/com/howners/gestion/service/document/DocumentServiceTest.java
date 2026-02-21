package com.howners.gestion.service.document;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private RentalRepository rentalRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private DocumentService documentService;

    private UUID userId;
    private UUID ownerId;
    private User currentUser;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        currentUser = User.builder()
                .id(userId)
                .email("tenant@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .passwordHash("hash")
                .role(Role.TENANT)
                .build();

        ownerUser = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .firstName("Marie")
                .lastName("Martin")
                .passwordHash("hash")
                .role(Role.OWNER)
                .build();

        setCurrentUser(userId, "tenant@test.com", "TENANT");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentUser(UUID id, String email, String role) {
        UserPrincipal principal = UserPrincipal.create(
                User.builder()
                        .id(id)
                        .email(email)
                        .passwordHash("hash")
                        .role(Role.valueOf(role))
                        .enabled(true)
                        .build()
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- Upload avec applicationId ---

    @Test
    void uploadDocument_withApplicationId_shouldLinkToApplication() throws IOException {
        UUID appId = UUID.randomUUID();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(storageService.uploadFile(any(), any(), any())).thenReturn("file-key-123");
        when(storageService.generatePresignedUrl("file-key-123")).thenReturn("https://presigned-url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "id-card.pdf", "application/pdf", "content".getBytes());

        DocumentResponse response = documentService.uploadDocument(
                file, DocumentType.IDENTITY, null, null, appId, "Ma carte d'identité");

        assertThat(response).isNotNull();
        assertThat(response.applicationId()).isEqualTo(appId);
        assertThat(response.documentType()).isEqualTo(DocumentType.IDENTITY);
        assertThat(response.fileUrl()).isEqualTo("https://presigned-url");

        verify(applicationRepository).findById(appId);
        verify(documentRepository).save(argThat(doc ->
                doc.getApplication() != null && doc.getApplication().getId().equals(appId)
        ));
    }

    @Test
    void uploadDocument_withApplicationId_notApplicant_shouldThrow() {
        UUID appId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Application application = Application.builder()
                .id(appId)
                .applicant(otherUser) // Different user
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        MockMultipartFile file = new MockMultipartFile(
                "file", "id-card.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(
                file, DocumentType.IDENTITY, null, null, appId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized to upload documents for this application");
    }

    @Test
    void uploadDocument_withApplicationId_notFound_shouldThrow() {
        UUID appId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "id-card.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(
                file, DocumentType.IDENTITY, null, null, appId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void uploadDocument_withoutApplicationId_shouldWork() throws IOException {
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(storageService.uploadFile(any(), any(), any())).thenReturn("file-key-456");
        when(storageService.generatePresignedUrl("file-key-456")).thenReturn("https://url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        DocumentResponse response = documentService.uploadDocument(
                file, DocumentType.OTHER, null, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.applicationId()).isNull();
        verify(applicationRepository, never()).findById(any());
    }

    @Test
    void uploadDocument_fileTooLarge_shouldThrow() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        assertThatThrownBy(() -> documentService.uploadDocument(
                file, DocumentType.OTHER, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("10MB");
    }

    // --- getApplicationDocuments ---

    @Test
    void getApplicationDocuments_asApplicant_shouldReturnDocuments() {
        UUID appId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser)
                .listing(listing)
                .build();

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .fileName("id.pdf")
                .fileKey("key1")
                .filePath("key1")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of(doc));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://presigned");

        List<DocumentResponse> result = documentService.getApplicationDocuments(appId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).documentType()).isEqualTo(DocumentType.IDENTITY);
        assertThat(result.get(0).fileName()).isEqualTo("id.pdf");
    }

    @Test
    void getApplicationDocuments_asOwner_shouldReturnDocuments() {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");

        UUID appId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser)
                .listing(listing)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(documentRepository.findByApplicationId(appId)).thenReturn(List.of());

        List<DocumentResponse> result = documentService.getApplicationDocuments(appId);

        assertThat(result).isEmpty();
    }

    @Test
    void getApplicationDocuments_asUnrelatedUser_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();
        setCurrentUser(otherUserId, "other@test.com", "TENANT");

        UUID appId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser) // Different from current
                .listing(listing)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> documentService.getApplicationDocuments(appId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // --- getDocument with application ---

    @Test
    void getDocument_linkedToApplication_asApplicant_shouldSucceed() {
        UUID docId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser)
                .listing(listing)
                .build();

        Document doc = Document.builder()
                .id(docId)
                .fileName("pay.pdf")
                .fileKey("key")
                .filePath("key")
                .documentType(DocumentType.PROOF_OF_INCOME)
                .application(application)
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storageService.generatePresignedUrl("key")).thenReturn("https://url");

        DocumentResponse response = documentService.getDocument(docId);

        assertThat(response).isNotNull();
        assertThat(response.applicationId()).isEqualTo(appId);
    }

    @Test
    void getDocument_linkedToApplication_asListingOwner_shouldSucceed() {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");

        UUID docId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(appId)
                .applicant(currentUser)
                .listing(listing)
                .build();

        Document doc = Document.builder()
                .id(docId)
                .fileName("pay.pdf")
                .fileKey("key")
                .filePath("key")
                .documentType(DocumentType.PROOF_OF_INCOME)
                .application(application)
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storageService.generatePresignedUrl("key")).thenReturn("https://url");

        DocumentResponse response = documentService.getDocument(docId);

        assertThat(response).isNotNull();
    }

    // --- deleteDocument with application ---

    @Test
    void deleteDocument_linkedToApplication_asApplicant_shouldSucceed() {
        UUID docId = UUID.randomUUID();
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .applicant(currentUser)
                .build();

        Document doc = Document.builder()
                .id(docId)
                .fileName("doc.pdf")
                .fileKey("key")
                .filePath("key")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(currentUser)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(docId);

        verify(storageService).deleteFile("key");
        verify(documentRepository).delete(doc);
    }

    @Test
    void deleteDocument_linkedToApplication_asUnrelatedUser_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();
        setCurrentUser(otherUserId, "other@test.com", "TENANT");

        UUID docId = UUID.randomUUID();
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .applicant(currentUser) // Not the current user
                .build();

        Document doc = Document.builder()
                .id(docId)
                .fileName("doc.pdf")
                .fileKey("key")
                .filePath("key")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(currentUser) // Also not the current user
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.deleteDocument(docId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized to delete");
    }

    // --- downloadDocument with application ---

    @Test
    void downloadDocument_linkedToApplication_asApplicant_shouldSucceed() throws IOException {
        UUID docId = UUID.randomUUID();
        Property property = Property.builder().id(UUID.randomUUID()).owner(ownerUser).name("Apt").build();
        Listing listing = Listing.builder().id(UUID.randomUUID()).property(property).title("Listing").build();
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .applicant(currentUser)
                .listing(listing)
                .build();

        Document doc = Document.builder()
                .id(docId)
                .fileName("doc.pdf")
                .fileKey("key")
                .filePath("key")
                .documentType(DocumentType.IDENTITY)
                .application(application)
                .uploader(currentUser)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storageService.downloadFile("key")).thenReturn("file-content".getBytes());

        byte[] result = documentService.downloadDocument(docId);

        assertThat(result).isEqualTo("file-content".getBytes());
    }
}
