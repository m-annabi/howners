package com.howners.gestion.service.auth;

import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.email.WelcomeOwnerEmailData;
import com.howners.gestion.dto.request.LoginRequest;
import com.howners.gestion.dto.request.RegisterRequest;
import com.howners.gestion.dto.request.UpdateProfileRequest;
import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.dto.response.AuthMessageResponse;
import com.howners.gestion.dto.response.AuthResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final com.howners.gestion.service.audit.AuditService auditService;
    private final com.howners.gestion.service.subscription.SubscriptionService subscriptionService;
    private final EmailService emailService;
    private final com.howners.gestion.service.referral.ReferralService referralService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final int VERIFICATION_TTL_HOURS = 24;

    // Réponse neutre, strictement identique que l'adresse soit nouvelle, déjà prise, ou invalide :
    // c'est ce qui supprime l'oracle d'énumération de comptes à l'inscription.
    private static final AuthMessageResponse VERIFICATION_SENT_RESPONSE = new AuthMessageResponse(
            "Si cette adresse peut être utilisée, un e-mail de vérification vient d'être envoyé. "
                    + "Consultez votre boîte de réception pour activer votre compte.");

    @Transactional
    public AuthMessageResponse register(RegisterRequest request) {
        // Auto-inscription publique : seuls OWNER et TENANT sont autorisés. ADMIN et
        // CONCIERGE ne peuvent pas être auto-attribués (élévation de privilèges) —
        // ces rôles sont accordés par un administrateur, jamais via ce formulaire.
        Role role = request.role();
        if (role != Role.OWNER && role != Role.TENANT) {
            throw new BadRequestException("Rôle d'inscription invalide : choisissez propriétaire ou locataire.");
        }

        // Anti-énumération : la réponse est identique que l'adresse existe déjà ou non. On ne lève
        // jamais « email déjà pris » et on n'auto-connecte plus — l'activation passe par l'e-mail.
        if (userRepository.existsByEmail(request.email())) {
            // Équilibrage grossier du temps de réponse : on effectue tout de même un hachage coûteux
            // (comme sur le chemin nominal) pour ne pas transformer la latence en oracle d'existence.
            passwordEncoder.encode(request.password());
            // On prévient le véritable titulaire (l'attaquant, lui, ne reçoit pas cet e-mail).
            try {
                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        request.email(), null,
                        "Tentative d'inscription avec votre adresse — Howners",
                        "Vous avez déjà un compte",
                        "Une inscription vient d'être tentée avec cette adresse, qui possède déjà un compte Howners. "
                                + "Si c'était vous, connectez-vous simplement. Sinon, vous pouvez ignorer ce message.",
                        null, "Se connecter", frontendUrl + "/login", false));
            } catch (Exception ignored) {
            }
            return VERIFICATION_SENT_RESPONSE;
        }

        String rawToken = generateVerificationToken();
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(role)
                .enabled(true)
                .emailVerified(false)
                .emailVerificationTokenHash(sha256Hex(rawToken))
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TTL_HOURS))
                .build();

        user = userRepository.save(user);

        // Auto-assigner le plan FREE
        subscriptionService.assignFreePlan(user.getId());

        // Generate a referral code for this user and, if signup came via a ref link, link it.
        try {
            referralService.ensureReferralCode(user.getId());
            referralService.recordReferral(request.referralCode(), user);
        } catch (Exception e) {
            // Never block registration on referral side-effects.
        }

        sendVerificationEmail(user, rawToken);

        return VERIFICATION_SENT_RESPONSE;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Connexion refusée tant que l'adresse n'est pas vérifiée.
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new BusinessException(
                    "Veuillez vérifier votre adresse e-mail avant de vous connecter. Un lien vous a été envoyé.");
        }

        String token = tokenProvider.generateToken(authentication);
        auditService.logAction(AuditAction.LOGIN, "User", user.getId());

        return new AuthResponse(token, jwtExpiration, UserResponse.from(user));
    }

    /** Confirme l'adresse à partir du token reçu par e-mail : le compte devient utilisable. */
    @Transactional
    public AuthMessageResponse verifyEmail(String rawToken) {
        String hash = sha256Hex(rawToken);
        User user = userRepository.findByEmailVerificationTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Lien de vérification invalide ou déjà utilisé."));

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Lien de vérification expiré. Demandez un nouveau lien.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        // E-mail de bienvenue déplacé ici (une fois l'adresse confirmée) pour les bailleurs.
        if (user.getRole() == Role.OWNER) {
            try {
                emailService.sendWelcomeOwnerEmail(WelcomeOwnerEmailData.builder()
                        .recipientEmail(user.getEmail())
                        .recipientName(user.getFirstName())
                        .dashboardUrl(frontendUrl + "/dashboard")
                        .addPropertyUrl(frontendUrl + "/properties/new")
                        .pricingUrl(frontendUrl + "/billing/pricing")
                        .build());
            } catch (Exception ignored) {
            }
        }

        return new AuthMessageResponse("Votre adresse e-mail est vérifiée. Vous pouvez maintenant vous connecter.");
    }

    /** Renvoie un lien de vérification. Réponse générique (aucune information sur l'existence du compte). */
    @Transactional
    public AuthMessageResponse resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.FALSE.equals(user.getEmailVerified())) {
                String rawToken = generateVerificationToken();
                user.setEmailVerificationTokenHash(sha256Hex(rawToken));
                user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TTL_HOURS));
                userRepository.save(user);
                sendVerificationEmail(user, rawToken);
            }
        });
        return VERIFICATION_SENT_RESPONSE;
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private void sendVerificationEmail(User user, String rawToken) {
        try {
            emailService.sendNotificationEmail(new GenericNotificationEmailData(
                    user.getEmail(),
                    user.getFirstName(),
                    "Vérifiez votre adresse e-mail — Howners",
                    "Confirmez votre adresse e-mail",
                    "Bienvenue sur Howners ! Pour activer votre compte, confirmez votre adresse en cliquant "
                            + "sur le bouton ci-dessous. Ce lien expire dans " + VERIFICATION_TTL_HOURS + " heures.",
                    null,
                    "Vérifier mon adresse",
                    frontendUrl + "/auth/verify-email?token=" + rawToken,
                    false));
        } catch (Exception e) {
            // best-effort : ne jamais faire échouer l'inscription sur un envoi d'e-mail.
        }
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserResponse.from(user);
    }

    /**
     * Révoque tous les jetons existants de l'utilisateur courant en incrémentant sa version de jeton.
     * Après cet appel, le JWT ayant servi à l'appeler (et tout autre jeton actif) est refusé par le
     * filtre : l'utilisateur doit se reconnecter. Utile en cas de suspicion de compromission.
     */
    @Transactional
    public void logoutAllSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException("User not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAddressLine1(request.addressLine1());
        user.setAddressLine2(request.addressLine2());
        user.setPostalCode(request.postalCode());
        user.setCity(request.city());
        user.setCountry(request.country());

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new BusinessException("User not authenticated");
    }

    /** Id de l'utilisateur courant, ou null s'il n'est pas authentifié (route publique). */
    public static UUID getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }
}
