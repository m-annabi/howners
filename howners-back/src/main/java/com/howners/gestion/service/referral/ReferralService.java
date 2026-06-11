package com.howners.gestion.service.referral;

import com.howners.gestion.domain.referral.Referral;
import com.howners.gestion.domain.referral.ReferralStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.referral.ReferralCodeResponse;
import com.howners.gestion.dto.referral.ReferralStatsResponse;
import com.howners.gestion.dto.referral.ReferralSummary;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.ReferralRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public String ensureReferralCode(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getReferralCode() != null && !user.getReferralCode().isBlank()) {
            return user.getReferralCode();
        }
        String code;
        int attempts = 0;
        do {
            code = generateCode();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Could not generate a unique referral code after 10 tries");
            }
        } while (userRepository.existsByReferralCode(code));
        user.setReferralCode(code);
        userRepository.save(user);
        log.info("Generated referral code {} for user {}", code, userId);
        return code;
    }

    /**
     * Record a referral relationship at signup time. Best-effort — never throws.
     */
    @Transactional
    public void recordReferral(String rawCode, User newUser) {
        if (rawCode == null || rawCode.isBlank()) return;
        String code = rawCode.trim().toUpperCase();
        userRepository.findByReferralCode(code).ifPresent(referrer -> {
            if (referrer.getId().equals(newUser.getId())) return;
            Referral r = Referral.builder()
                    .referrer(referrer)
                    .referee(newUser)
                    .status(ReferralStatus.PENDING)
                    .build();
            referralRepository.save(r);
            log.info("Referral recorded — referrer={} referee={}", referrer.getEmail(), newUser.getEmail());
        });
    }

    /**
     * Mark a referee's record as CONVERTED — called when a paid subscription is activated.
     */
    @Transactional
    public void markConverted(UUID refereeId) {
        referralRepository.findAll().stream()
                .filter(r -> r.getReferee().getId().equals(refereeId))
                .filter(r -> r.getStatus() == ReferralStatus.PENDING)
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(ReferralStatus.CONVERTED);
                    r.setConvertedAt(LocalDateTime.now());
                    referralRepository.save(r);
                    log.info("Referral marked CONVERTED for referee {}", refereeId);
                });
    }

    @Transactional
    public ReferralSummary getMySummary() {
        UUID userId = AuthService.getCurrentUserId();
        String code = ensureReferralCode(userId);
        List<Referral> all = referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId);
        long pending = all.stream().filter(r -> r.getStatus() == ReferralStatus.PENDING).count();
        long converted = all.stream().filter(r -> r.getStatus() == ReferralStatus.CONVERTED).count();

        return new ReferralSummary(
                code,
                frontendUrl + "/auth/register?ref=" + code,
                pending,
                converted,
                all.stream().map(r -> new ReferralSummary.RefereeItem(
                        r.getReferee().getFullName(),
                        r.getStatus().name(),
                        r.getCreatedAt()
                )).collect(Collectors.toList())
        );
    }

    /**
     * Returns the current user's referral code and shareable link.
     */
    @Transactional
    public ReferralCodeResponse getMyCode() {
        UUID userId = AuthService.getCurrentUserId();
        String code = ensureReferralCode(userId);
        String link = frontendUrl + "/auth/register?ref=" + code;
        return new ReferralCodeResponse(code, link);
    }

    /**
     * Returns referral statistics for the current user.
     */
    @Transactional(readOnly = true)
    public ReferralStatsResponse getReferralStats() {
        UUID userId = AuthService.getCurrentUserId();
        List<Referral> all = referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId);
        long pending = all.stream().filter(r -> r.getStatus() == ReferralStatus.PENDING).count();
        long converted = all.stream().filter(r -> r.getStatus() == ReferralStatus.CONVERTED).count();
        long total = all.size();
        return new ReferralStatsResponse(total, converted, pending);
    }

    /**
     * Apply a referral code — links the current authenticated user to the referrer.
     * Used when a user signs up without the ref query parameter and wants to apply a code later.
     */
    @Transactional
    public void applyReferralCode(String code) {
        UUID userId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        // Check if this user was already referred
        boolean alreadyReferred = referralRepository.findAll().stream()
                .anyMatch(r -> r.getReferee().getId().equals(userId));
        if (alreadyReferred) {
            throw new BusinessException("Vous avez déjà utilisé un code de parrainage.");
        }

        String normalised = code.trim().toUpperCase();
        User referrer = userRepository.findByReferralCode(normalised)
                .orElseThrow(() -> new BusinessException("Code de parrainage invalide."));

        if (referrer.getId().equals(userId)) {
            throw new BusinessException("Vous ne pouvez pas utiliser votre propre code.");
        }

        Referral r = Referral.builder()
                .referrer(referrer)
                .referee(currentUser)
                .status(ReferralStatus.PENDING)
                .build();
        referralRepository.save(r);
        log.info("Referral applied via code — referrer={} referee={}", referrer.getEmail(), currentUser.getEmail());
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
