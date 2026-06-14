package com.howners.gestion.service.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.domain.inventory.EdlComparaison;
import com.howners.gestion.domain.inventory.EtatDesLieux;
import com.howners.gestion.domain.inventory.EtatDesLieuxType;
import com.howners.gestion.domain.inventory.StatutComparaison;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.inventory.ComparaisonEdlResponse;
import com.howners.gestion.dto.inventory.RetenueDepotRequest;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.EdlComparaisonRepository;
import com.howners.gestion.repository.EtatDesLieuxRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdlComparisonServiceTest {

    @Mock EdlComparaisonRepository comparaisonRepository;
    @Mock EtatDesLieuxRepository etatDesLieuxRepository;
    @Mock RentalRepository rentalRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock PdfService pdfService;
    @Mock StorageService storageService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks EdlComparisonService comparisonService;

    UUID ownerId;
    UUID rentalId;
    Rental rental;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        rentalId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).role(Role.OWNER).build();
        Property property = new Property();
        property.setOwner(owner);
        property.setName("Bien Test");
        rental = Rental.builder()
                .id(rentalId)
                .property(property)
                .depositAmount(new BigDecimal("1000.00"))
                .build();

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        lenient().when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        lenient().when(comparaisonRepository.save(any(EdlComparaison.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private EtatDesLieux edl(EtatDesLieuxType type, String rooms) {
        return EtatDesLieux.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .type(type)
                .inspectionDate(LocalDate.now())
                .roomConditions(rooms)
                .keysCount(2)
                .build();
    }

    @Test
    void detecteLesDegradationsParPiece() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.empty());
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.ENTREE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.ENTREE,
                        "[{\"name\":\"Salon\",\"condition\":\"BON\",\"comments\":\"\"},{\"name\":\"Cuisine\",\"condition\":\"NEUF\",\"comments\":\"\"}]")));
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.SORTIE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.SORTIE,
                        "[{\"name\":\"salon \",\"condition\":\"MAUVAIS\",\"comments\":\"trou au mur\"},{\"name\":\"Cuisine\",\"condition\":\"NEUF\",\"comments\":\"\"}]")));

        ComparaisonEdlResponse response = comparisonService.comparer(rentalId);

        assertThat(response.pieces()).hasSize(2);
        var salon = response.pieces().stream().filter(p -> p.nom().equalsIgnoreCase("Salon")).findFirst().orElseThrow();
        assertThat(salon.degradee()).isTrue();
        var cuisine = response.pieces().stream().filter(p -> p.nom().equals("Cuisine")).findFirst().orElseThrow();
        assertThat(cuisine.degradee()).isFalse();
        assertThat(response.soldeARestituer()).isEqualByComparingTo("1000.00");
    }

    @Test
    void signaleLesPiecesNonComparables() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.empty());
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.ENTREE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.ENTREE,
                        "[{\"name\":\"Cave\",\"condition\":\"BON\",\"comments\":\"\"}]")));
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.SORTIE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.SORTIE,
                        "[{\"name\":\"Grenier\",\"condition\":\"BON\",\"comments\":\"\"}]")));

        ComparaisonEdlResponse response = comparisonService.comparer(rentalId);

        assertThat(response.pieces()).hasSize(2);
        assertThat(response.pieces()).allMatch(ComparaisonEdlResponse.PieceComparee::nonComparable);
    }

    @Test
    void refuseSiEdlSortieManquant() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.empty());
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.ENTREE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.ENTREE, "[]")));
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.SORTIE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> comparisonService.comparer(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sortie");
    }

    @Test
    void refuseSiJsonMalforme() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.empty());
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.ENTREE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.ENTREE, "pas du json")));
        when(etatDesLieuxRepository.findByRentalIdAndType(rentalId, EtatDesLieuxType.SORTIE))
                .thenReturn(Optional.of(edl(EtatDesLieuxType.SORTIE, "[]")));

        assertThatThrownBy(() -> comparisonService.comparer(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("illisible");
    }

    @Test
    void plafonneLesRetenuesAuDepot() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.of(
                EdlComparaison.builder()
                        .rental(rental)
                        .edlEntree(edl(EtatDesLieuxType.ENTREE, "[]"))
                        .edlSortie(edl(EtatDesLieuxType.SORTIE, "[]"))
                        .statut(StatutComparaison.BROUILLON)
                        .build()));

        RetenueDepotRequest request = new RetenueDepotRequest(List.of(
                new RetenueDepotRequest.Retenue("Salon", "BON", "MAUVAIS", "Trou au mur",
                        new BigDecimal("1200.00"))));

        assertThatThrownBy(() -> comparisonService.enregistrerRetenues(rentalId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void enregistreLesRetenuesEtCalculeLeSolde() {
        when(comparaisonRepository.findByRentalId(rentalId)).thenReturn(Optional.of(
                EdlComparaison.builder()
                        .rental(rental)
                        .edlEntree(edl(EtatDesLieuxType.ENTREE,
                                "[{\"name\":\"Salon\",\"condition\":\"BON\",\"comments\":\"\"}]"))
                        .edlSortie(edl(EtatDesLieuxType.SORTIE,
                                "[{\"name\":\"Salon\",\"condition\":\"MAUVAIS\",\"comments\":\"\"}]"))
                        .statut(StatutComparaison.BROUILLON)
                        .build()));

        RetenueDepotRequest request = new RetenueDepotRequest(List.of(
                new RetenueDepotRequest.Retenue("Salon", "BON", "MAUVAIS", "Trou au mur",
                        new BigDecimal("350.00"))));

        ComparaisonEdlResponse response = comparisonService.enregistrerRetenues(rentalId, request);

        assertThat(response.totalRetenues()).isEqualByComparingTo("350.00");
        assertThat(response.soldeARestituer()).isEqualByComparingTo("650.00");
        assertThat(response.retenues()).hasSize(1);
    }
}
