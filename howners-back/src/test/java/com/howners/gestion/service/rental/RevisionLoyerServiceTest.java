package com.howners.gestion.service.rental;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.IrlIndice;
import com.howners.gestion.domain.rental.RentRevision;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.rental.StatutRevision;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rental.RevisionLoyerResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.IrlIndiceRepository;
import com.howners.gestion.repository.RentRevisionRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionLoyerServiceTest {

    @Mock RentRevisionRepository revisionRepository;
    @Mock IrlIndiceRepository irlIndiceRepository;
    @Mock RentalRepository rentalRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock PdfService pdfService;
    @Mock StorageService storageService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;

    @InjectMocks RevisionLoyerService revisionLoyerService;

    UUID ownerId;
    UUID rentalId;
    User owner;
    Rental rental;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        rentalId = UUID.randomUUID();
        owner = User.builder().id(ownerId).email("owner@test.fr").firstName("Pro").lastName("Prio")
                .role(Role.OWNER).build();
        Property property = new Property();
        property.setOwner(owner);
        property.setName("Appartement Test");
        rental = Rental.builder()
                .id(rentalId)
                .property(property)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.of(2023, 2, 15)) // T1
                .monthlyRent(new BigDecimal("800.00"))
                .build();

        UserPrincipal principal = new UserPrincipal(ownerId, "owner@test.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        lenient().when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void calculeLaRevisionSelonLaVariationIrl() {
        IrlIndice ancien = IrlIndice.builder().annee(2023).trimestre(1).valeur(new BigDecimal("138.61")).build();
        IrlIndice nouveau = IrlIndice.builder().annee(2024).trimestre(1).valeur(new BigDecimal("143.46")).build();
        when(revisionRepository.existsByRentalIdAndDateRevisionAfterAndStatutNot(eq(rentalId), any(), eq(StatutRevision.ANNULEE)))
                .thenReturn(false);
        when(irlIndiceRepository.findTopByTrimestreOrderByAnneeDesc(1)).thenReturn(Optional.of(nouveau));
        when(irlIndiceRepository.findByAnneeAndTrimestre(2023, 1)).thenReturn(Optional.of(ancien));
        when(revisionRepository.save(any(RentRevision.class))).thenAnswer(inv -> inv.getArgument(0));

        RevisionLoyerResponse response = revisionLoyerService.calculerRevision(rentalId);

        // 800 × 143.46 / 138.61 = 827.99
        assertThat(response.nouveauLoyer()).isEqualByComparingTo("827.99");
        assertThat(response.ancienLoyer()).isEqualByComparingTo("800.00");
        assertThat(response.statut()).isEqualTo(StatutRevision.BROUILLON);
    }

    @Test
    void refuseSiIndiceManquant() {
        when(revisionRepository.existsByRentalIdAndDateRevisionAfterAndStatutNot(eq(rentalId), any(), eq(StatutRevision.ANNULEE)))
                .thenReturn(false);
        when(irlIndiceRepository.findTopByTrimestreOrderByAnneeDesc(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revisionLoyerService.calculerRevision(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("indice IRL");
    }

    @Test
    void refuseSiRevisionRecente() {
        when(revisionRepository.existsByRentalIdAndDateRevisionAfterAndStatutNot(eq(rentalId), any(), eq(StatutRevision.ANNULEE)))
                .thenReturn(true);

        assertThatThrownBy(() -> revisionLoyerService.calculerRevision(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moins d'un an");
    }

    @Test
    void refuseSiLocationInactive() {
        rental.setStatus(RentalStatus.TERMINATED);

        assertThatThrownBy(() -> revisionLoyerService.calculerRevision(rentalId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void appliquerMetAJourLeLoyerDuBail() {
        IrlIndice ancien = IrlIndice.builder().annee(2023).trimestre(1).valeur(new BigDecimal("138.61")).build();
        IrlIndice nouveau = IrlIndice.builder().annee(2024).trimestre(1).valeur(new BigDecimal("143.46")).build();
        UUID revisionId = UUID.randomUUID();
        RentRevision revision = RentRevision.builder()
                .id(revisionId)
                .rental(rental)
                .ancienLoyer(new BigDecimal("800.00"))
                .nouveauLoyer(new BigDecimal("827.99"))
                .indiceAncien(ancien)
                .indiceNouveau(nouveau)
                .dateRevision(LocalDate.now())
                .dateEffet(LocalDate.now())
                .statut(StatutRevision.NOTIFIEE)
                .build();
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(revisionRepository.save(any(RentRevision.class))).thenAnswer(inv -> inv.getArgument(0));

        RevisionLoyerResponse response = revisionLoyerService.appliquerRevision(revisionId);

        assertThat(rental.getMonthlyRent()).isEqualByComparingTo("827.99");
        assertThat(response.statut()).isEqualTo(StatutRevision.APPLIQUEE);
        verify(rentalRepository).save(rental);
    }

    @Test
    void refuseDAppliquerUnBrouillon() {
        UUID revisionId = UUID.randomUUID();
        RentRevision revision = RentRevision.builder()
                .id(revisionId)
                .rental(rental)
                .ancienLoyer(new BigDecimal("800.00"))
                .nouveauLoyer(new BigDecimal("827.99"))
                .dateRevision(LocalDate.now())
                .statut(StatutRevision.BROUILLON)
                .build();
        when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        assertThatThrownBy(() -> revisionLoyerService.appliquerRevision(revisionId))
                .isInstanceOf(BadRequestException.class);
    }
}
