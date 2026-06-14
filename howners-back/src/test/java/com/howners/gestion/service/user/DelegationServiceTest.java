package com.howners.gestion.service.user;

import com.howners.gestion.domain.user.CompteDelegation;
import com.howners.gestion.domain.user.StatutDelegation;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.user.CreateDelegationRequest;
import com.howners.gestion.dto.user.DelegationResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.CompteDelegationRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.payment.FinancialDashboardService;
import com.howners.gestion.service.subscription.FeatureGateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegationServiceTest {

    @Mock CompteDelegationRepository delegationRepository;
    @Mock UserRepository userRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock RentalRepository rentalRepository;
    @Mock FeatureGateService featureGateService;
    @Mock FinancialDashboardService financialDashboardService;
    @Mock NotificationService notificationService;

    @InjectMocks DelegationService delegationService;

    UUID agenceId;
    UUID delegueId;
    User agence;
    User delegue;

    @BeforeEach
    void setup() {
        agenceId = UUID.randomUUID();
        delegueId = UUID.randomUUID();
        agence = User.builder().id(agenceId).email("agence@test.fr").firstName("Agence").lastName("Immo").build();
        delegue = User.builder().id(delegueId).email("collab@test.fr").firstName("Col").lastName("Lab").build();

        UserPrincipal principal = new UserPrincipal(agenceId, "agence@test.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void inviterRefuseSansFeatureMultiAccount() {
        when(featureGateService.hasFeature(agenceId, "multi_account")).thenReturn(false);

        assertThatThrownBy(() -> delegationService.inviterParEmail(new CreateDelegationRequest("collab@test.fr")))
                .isInstanceOf(ForbiddenException.class);
        verify(delegationRepository, never()).save(any());
    }

    @Test
    void inviterCreeUneDelegationActive() {
        when(featureGateService.hasFeature(agenceId, "multi_account")).thenReturn(true);
        when(userRepository.findById(agenceId)).thenReturn(Optional.of(agence));
        when(userRepository.findByEmail("collab@test.fr")).thenReturn(Optional.of(delegue));
        when(delegationRepository.findByAgenceIdAndDelegueId(agenceId, delegueId)).thenReturn(Optional.empty());
        when(delegationRepository.save(any(CompteDelegation.class))).thenAnswer(inv -> inv.getArgument(0));

        DelegationResponse response = delegationService.inviterParEmail(new CreateDelegationRequest("collab@test.fr"));

        assertThat(response.statut()).isEqualTo(StatutDelegation.ACTIVE);
        assertThat(response.delegueEmail()).isEqualTo("collab@test.fr");
        verify(notificationService).create(any(), any(), any(), any(), any());
    }

    @Test
    void inviterRefuseLAutoDelegation() {
        when(featureGateService.hasFeature(agenceId, "multi_account")).thenReturn(true);
        when(userRepository.findById(agenceId)).thenReturn(Optional.of(agence));
        when(userRepository.findByEmail("agence@test.fr")).thenReturn(Optional.of(agence));

        assertThatThrownBy(() -> delegationService.inviterParEmail(new CreateDelegationRequest("agence@test.fr")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void inviterRefuseSiDelegationDejaActive() {
        when(featureGateService.hasFeature(agenceId, "multi_account")).thenReturn(true);
        when(userRepository.findById(agenceId)).thenReturn(Optional.of(agence));
        when(userRepository.findByEmail("collab@test.fr")).thenReturn(Optional.of(delegue));
        CompteDelegation existing = CompteDelegation.builder()
                .agence(agence).delegue(delegue).statut(StatutDelegation.ACTIVE).build();
        when(delegationRepository.findByAgenceIdAndDelegueId(agenceId, delegueId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> delegationService.inviterParEmail(new CreateDelegationRequest("collab@test.fr")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void apercuRefuseSiPasLeDelegue() {
        UUID delegationId = UUID.randomUUID();
        User autre = User.builder().id(UUID.randomUUID()).build();
        CompteDelegation delegation = CompteDelegation.builder()
                .id(delegationId).agence(delegue).delegue(autre).statut(StatutDelegation.ACTIVE).build();
        when(delegationRepository.findById(delegationId)).thenReturn(Optional.of(delegation));

        assertThatThrownBy(() -> delegationService.getApercuCompte(delegationId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void apercuRefuseSiRevoquee() {
        UUID delegationId = UUID.randomUUID();
        CompteDelegation delegation = CompteDelegation.builder()
                .id(delegationId).agence(delegue).delegue(agence).statut(StatutDelegation.REVOQUEE).build();
        when(delegationRepository.findById(delegationId)).thenReturn(Optional.of(delegation));

        assertThatThrownBy(() -> delegationService.getApercuCompte(delegationId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void revoquerRefuseSiPasLAgence() {
        UUID delegationId = UUID.randomUUID();
        User autre = User.builder().id(UUID.randomUUID()).build();
        CompteDelegation delegation = CompteDelegation.builder()
                .id(delegationId).agence(autre).delegue(delegue).statut(StatutDelegation.ACTIVE).build();
        when(delegationRepository.findById(delegationId)).thenReturn(Optional.of(delegation));

        assertThatThrownBy(() -> delegationService.revoquer(delegationId))
                .isInstanceOf(ForbiddenException.class);
        verify(delegationRepository, never()).save(any());
    }
}
