package com.howners.gestion.service.auth;

import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.request.LoginRequest;
import com.howners.gestion.dto.request.RegisterRequest;
import com.howners.gestion.dto.response.AuthMessageResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.security.jwt.JwtTokenProvider;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.referral.ReferralService;
import com.howners.gestion.service.subscription.SubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;
    @Mock AuditService auditService;
    @Mock SubscriptionService subscriptionService;
    @Mock EmailService emailService;
    @Mock ReferralService referralService;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:4200");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private RegisterRequest registerRequest(String email) {
        return new RegisterRequest(email, "Password1", "Jean", "Dupont", "0600000000", Role.OWNER, null);
    }

    @Test
    void register_nouvelleAdresse_creeUnCompteNonVerifieEtEnvoieLeMailSansJeton() {
        when(userRepository.existsByEmail("new@test.fr")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthMessageResponse resp = authService.register(registerRequest("new@test.fr"));

        // Réponse générique, aucun jeton, aucune auto-connexion.
        assertThat(resp.message()).contains("e-mail de vérification");
        verify(tokenProvider, never()).generateToken(any());
        verify(authenticationManager, never()).authenticate(any());
        verify(emailService).sendNotificationEmail(any());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmailVerified()).isFalse();
        assertThat(saved.getValue().getEmailVerificationTokenHash()).isNotBlank();
        assertThat(saved.getValue().getEmailVerificationExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void register_adresseExistante_memeReponseGeneriqueSansCreerDeCompteNiErreur() {
        when(userRepository.existsByEmail("taken@test.fr")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        AuthMessageResponse resp = authService.register(registerRequest("taken@test.fr"));

        // Aucune fuite d'existence : ni exception, ni nouveau compte, message identique.
        assertThat(resp.message()).contains("e-mail de vérification");
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_tokenValide_marqueLAdresseVerifiee() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("a@test.fr").role(Role.TENANT)
                .emailVerified(false)
                .emailVerificationTokenHash("hash")
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(userRepository.findByEmailVerificationTokenHash(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.verifyEmail("raw-token");

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
    }

    @Test
    void verifyEmail_tokenExpire_estRejete() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("a@test.fr").role(Role.TENANT)
                .emailVerified(false)
                .emailVerificationTokenHash("hash")
                .emailVerificationExpiresAt(LocalDateTime.now().minusHours(1))
                .build();
        when(userRepository.findByEmailVerificationTokenHash(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail("raw-token"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void login_adresseNonVerifiee_estBloquee() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "a@test.fr", "x", "OWNER", true, 0);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User user = User.builder().id(userId).email("a@test.fr").role(Role.OWNER).emailVerified(false).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@test.fr", "Password1")))
                .isInstanceOf(BusinessException.class);
        verify(tokenProvider, never()).generateToken(any());
    }
}
