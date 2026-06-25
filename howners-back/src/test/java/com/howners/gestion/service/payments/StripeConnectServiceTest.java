package com.howners.gestion.service.payments;

import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeConnectServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private StripeConnectService stripeConnectService;

    private User ownerWithStatus(String status) {
        return User.builder().id(UUID.randomUUID()).email("owner@test.com")
                .firstName("Marie").lastName("Martin").passwordHash("h").role(Role.OWNER).enabled(true)
                .stripeConnectAccountId("acct_1").stripeConnectStatus(status).build();
    }

    @Test
    void processAccountUpdate_chargesAndPayoutsEnabled_setsCompleted() {
        User owner = ownerWithStatus("PENDING");
        when(userRepository.findByStripeConnectAccountId("acct_1")).thenReturn(Optional.of(owner));

        stripeConnectService.processAccountUpdate("acct_1", true, true);

        assertThat(owner.getStripeConnectStatus()).isEqualTo("COMPLETED");
        verify(userRepository).save(owner);
    }

    @Test
    void processAccountUpdate_notFullyEnabled_setsPending() {
        User owner = ownerWithStatus("NONE");
        when(userRepository.findByStripeConnectAccountId("acct_1")).thenReturn(Optional.of(owner));

        stripeConnectService.processAccountUpdate("acct_1", true, false);

        assertThat(owner.getStripeConnectStatus()).isEqualTo("PENDING");
        verify(userRepository).save(owner);
    }

    @Test
    void processAccountUpdate_statusUnchanged_doesNotSave() {
        User owner = ownerWithStatus("COMPLETED");
        when(userRepository.findByStripeConnectAccountId("acct_1")).thenReturn(Optional.of(owner));

        stripeConnectService.processAccountUpdate("acct_1", true, true);

        verify(userRepository, never()).save(any());
    }

    @Test
    void processAccountUpdate_unknownAccount_isNoop() {
        when(userRepository.findByStripeConnectAccountId("acct_unknown")).thenReturn(Optional.empty());

        stripeConnectService.processAccountUpdate("acct_unknown", true, true);

        verify(userRepository, never()).save(any());
    }
}
