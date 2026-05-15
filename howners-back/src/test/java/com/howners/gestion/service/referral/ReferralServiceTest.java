package com.howners.gestion.service.referral;

import com.howners.gestion.domain.referral.Referral;
import com.howners.gestion.domain.referral.ReferralStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.referral.ReferralSummary;
import com.howners.gestion.repository.ReferralRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock UserRepository userRepository;
    @Mock ReferralRepository referralRepository;
    @InjectMocks ReferralService referralService;

    UUID referrerId;
    UUID newUserId;
    User referrer;
    User newUser;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(referralService, "frontendUrl", "http://localhost:4200");
        referrerId = UUID.randomUUID();
        newUserId = UUID.randomUUID();
        referrer = User.builder().id(referrerId).email("a@b.test").firstName("Anna").lastName("Annabi").build();
        newUser = User.builder().id(newUserId).email("c@d.test").firstName("Carl").lastName("Newman").build();

        // Mock auth context — getMySummary uses AuthService.getCurrentUserId() statically.
        UserPrincipal principal = new UserPrincipal(referrerId, "a@b.test", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void ensureReferralCode_generatesAndPersists_whenAbsent() {
        when(userRepository.findById(referrerId)).thenReturn(Optional.of(referrer));
        when(userRepository.existsByReferralCode(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = referralService.ensureReferralCode(referrerId);

        assertThat(code).hasSize(7);                       // CODE_LENGTH is 7
        assertThat(code).matches("[A-Z2-9]{7}");           // exclusive of 0/1/I/O
        assertThat(referrer.getReferralCode()).isEqualTo(code);
        verify(userRepository).save(referrer);
    }

    @Test
    void ensureReferralCode_isIdempotent_whenCodeAlreadySet() {
        referrer.setReferralCode("EXIST01");
        when(userRepository.findById(referrerId)).thenReturn(Optional.of(referrer));

        String code = referralService.ensureReferralCode(referrerId);

        assertThat(code).isEqualTo("EXIST01");
        verify(userRepository, never()).save(any());
    }

    @Test
    void recordReferral_doesNothing_whenCodeIsBlankOrNull() {
        referralService.recordReferral(null, newUser);
        referralService.recordReferral("   ", newUser);
        verify(referralRepository, never()).save(any());
    }

    @Test
    void recordReferral_ignoresUnknownCode() {
        when(userRepository.findByReferralCode("ZZZZZZZ")).thenReturn(Optional.empty());
        referralService.recordReferral("zzzzzzz", newUser); // lowercase → upper
        verify(referralRepository, never()).save(any());
    }

    @Test
    void recordReferral_rejectsSelfReferral() {
        newUser.setId(referrerId);                              // referee == referrer
        when(userRepository.findByReferralCode("CODE001")).thenReturn(Optional.of(referrer));
        referralService.recordReferral("CODE001", newUser);
        verify(referralRepository, never()).save(any());
    }

    @Test
    void recordReferral_persistsPendingRow_whenCodeMatches() {
        when(userRepository.findByReferralCode("ABCDEFG")).thenReturn(Optional.of(referrer));

        referralService.recordReferral("abcdefg", newUser); // lower → upper

        ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
        verify(referralRepository).save(captor.capture());
        Referral saved = captor.getValue();
        assertThat(saved.getReferrer()).isEqualTo(referrer);
        assertThat(saved.getReferee()).isEqualTo(newUser);
        assertThat(saved.getStatus()).isEqualTo(ReferralStatus.PENDING);
    }

    @Test
    void getMySummary_returnsCodeShareUrlAndCounts() {
        referrer.setReferralCode("MYCODE1");
        when(userRepository.findById(referrerId)).thenReturn(Optional.of(referrer));

        Referral converted = Referral.builder()
                .referrer(referrer)
                .referee(newUser)
                .status(ReferralStatus.CONVERTED)
                .build();
        Referral pending = Referral.builder()
                .referrer(referrer)
                .referee(User.builder().id(UUID.randomUUID()).firstName("P").lastName("E").build())
                .status(ReferralStatus.PENDING)
                .build();
        when(referralRepository.findByReferrerIdOrderByCreatedAtDesc(referrerId))
                .thenReturn(List.of(converted, pending));

        ReferralSummary summary = referralService.getMySummary();

        assertThat(summary.code()).isEqualTo("MYCODE1");
        assertThat(summary.shareUrl()).isEqualTo("http://localhost:4200/auth/register?ref=MYCODE1");
        assertThat(summary.convertedCount()).isEqualTo(1);
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.referees()).hasSize(2);
    }
}
