package com.howners.gestion.service.rgpd;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.message.Message;
import com.howners.gestion.domain.rgpd.RgpdRequest;
import com.howners.gestion.domain.rgpd.RgpdRequestStatus;
import com.howners.gestion.domain.rgpd.RgpdRequestType;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.*;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RgpdServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserConsentRepository userConsentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock RentalRepository rentalRepository;
    @Mock ContractRepository contractRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock DocumentRepository documentRepository;
    @Mock MessageRepository messageRepository;
    @Mock RgpdRequestRepository rgpdRequestRepository;
    @Mock AuditService auditService;
    @Mock PdfService pdfService;
    @Mock StorageService storageService;

    @InjectMocks RgpdService rgpdService;

    UUID userId;
    User user;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId).email("jean@test.fr").firstName("Jean").lastName("Dupont")
                .phone("0600000000").role(Role.TENANT).build();

        UserPrincipal principal = new UserPrincipal(userId, "jean@test.fr", "x", "TENANT", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(rgpdRequestRepository.save(any(RgpdRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private Document doc(DocumentType type, String key, String name) {
        return Document.builder().id(UUID.randomUUID()).documentType(type)
                .fileKey(key).fileName(name).filePath("/p/" + name).build();
    }

    @Test
    void effacement_conserveLeLegal_supprimeLePii() {
        Document bail = doc(DocumentType.CONTRACT, "k-bail", "bail.pdf");
        Document cni = doc(DocumentType.ID_CARD, "k-cni", "CNI_Jean_Dupont.pdf");
        when(documentRepository.findByUploaderId(userId)).thenReturn(List.of(bail, cni));
        when(messageRepository.findBySenderId(userId)).thenReturn(List.of());

        rgpdService.anonymizeUser();

        // Bail conservé sous legal hold, fichier S3 intact
        assertThat(bail.getLegalHold()).isTrue();
        assertThat(bail.getFileKey()).isEqualTo("k-bail");
        verify(storageService, never()).deleteFile("k-bail");

        // CNI : fichier S3 supprimé + métadonnées de la ligne effacées
        verify(storageService).deleteFile("k-cni");
        assertThat(cni.getFileName()).isEqualTo("[supprimé-rgpd]");
        assertThat(cni.getFileKey()).isEqualTo("[supprimé-rgpd]");
        assertThat(cni.getDescription()).isNull();
        assertThat(cni.getIsArchived()).isTrue();

        // Compte pseudonymisé
        assertThat(user.getIsAnonymized()).isTrue();
        assertThat(user.getEmail()).startsWith("anonymized-");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getEnabled()).isFalse();

        // Demande tracée ERASURE + clôturée
        ArgumentCaptor<RgpdRequest> cap = ArgumentCaptor.forClass(RgpdRequest.class);
        verify(rgpdRequestRepository, atLeastOnce()).save(cap.capture());
        RgpdRequest req = cap.getValue();
        assertThat(req.getType()).isEqualTo(RgpdRequestType.ERASURE);
        assertThat(req.getStatus()).isEqualTo(RgpdRequestStatus.COMPLETED);
        assertThat(req.getCompletedAt()).isNotNull();
        verify(auditService).logAction(eq(AuditAction.DATA_ERASURE), eq("User"), eq(userId));
    }

    @Test
    void effacement_anonymiseLesMessagesEmis() {
        Message m = Message.builder().id(UUID.randomUUID())
                .subject("Bonjour").body("Voici mes coordonnées : 0600000000").build();
        when(documentRepository.findByUploaderId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderId(userId)).thenReturn(List.of(m));

        rgpdService.anonymizeUser();

        assertThat(m.getBody()).isEqualTo("[Supprimé à la demande de l'utilisateur]");
        assertThat(m.getSubject()).isNull();
        verify(messageRepository).save(m);
    }

    @Test
    void effacement_refuseSiDejaAnonymise() {
        user.setIsAnonymized(true);

        assertThatThrownBy(() -> rgpdService.anonymizeUser())
                .isInstanceOf(BusinessException.class);

        verify(storageService, never()).deleteFile(any());
        verify(rgpdRequestRepository, never()).save(any());
        verify(documentRepository, never()).findByUploaderId(any());
    }

    @Test
    void export_traceUneDemandeExportCompletee() {
        when(propertyRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(rentalRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(contractRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(paymentRepository.findByPayerId(userId)).thenReturn(List.of());
        when(documentRepository.findByUploaderId(userId)).thenReturn(List.of());
        when(userConsentRepository.findByUserId(userId)).thenReturn(List.of());

        rgpdService.exportUserData();

        ArgumentCaptor<RgpdRequest> cap = ArgumentCaptor.forClass(RgpdRequest.class);
        verify(rgpdRequestRepository).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(RgpdRequestType.EXPORT);
        assertThat(cap.getValue().getStatus()).isEqualTo(RgpdRequestStatus.COMPLETED);
        verify(auditService).logAction(eq(AuditAction.DATA_EXPORT), eq("User"), eq(userId));
    }

    @Test
    void archive_zipContientJsonNoticeEtFichiers() throws Exception {
        Document photo = doc(DocumentType.PHOTOS, "k-photo", "salon.png");
        photo.setCreatedAt(LocalDateTime.now()); // exportUserData sérialise createdAt
        when(propertyRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(rentalRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(contractRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(paymentRepository.findByPayerId(userId)).thenReturn(List.of());
        when(userConsentRepository.findByUserId(userId)).thenReturn(List.of());
        when(documentRepository.findByUploaderId(userId)).thenReturn(List.of(photo));
        when(storageService.downloadFile("k-photo")).thenReturn(new byte[]{1, 2, 3, 4});

        byte[] zipBytes = rgpdService.exportUserDataAsArchive();

        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                entries.add(e.getName());
            }
        }
        assertThat(entries).contains("export.json", "LISEZMOI.txt");
        assertThat(entries).anyMatch(n -> n.startsWith("documents/") && n.endsWith("_salon.png"));
        verify(storageService).downloadFile("k-photo");
    }

    @Test
    void archive_ignoreUnFichierIndisponibleSansEchouer() throws Exception {
        Document cni = doc(DocumentType.ID_CARD, "k-cni", "cni.pdf");
        cni.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(rentalRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(contractRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(paymentRepository.findByPayerId(userId)).thenReturn(List.of());
        when(userConsentRepository.findByUserId(userId)).thenReturn(List.of());
        when(documentRepository.findByUploaderId(userId)).thenReturn(List.of(cni));
        when(storageService.downloadFile("k-cni")).thenThrow(new RuntimeException("S3 indisponible"));

        byte[] zipBytes = rgpdService.exportUserDataAsArchive();

        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) entries.add(e.getName());
        }
        // L'archive est tout de même produite, sans le fichier manquant.
        assertThat(entries).contains("export.json");
        assertThat(entries).noneMatch(n -> n.startsWith("documents/"));
    }
}
