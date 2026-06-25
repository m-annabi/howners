package com.howners.gestion.service.payments;

import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.payments.StripeConnectStatusResponse;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Onboarding Stripe Connect (Express). Permet à un bailleur de recevoir
 * directement les loyers sur son compte bancaire via la plateforme.
 *
 * Pré-requis business :
 *  - Activer Stripe Connect sur le compte plateforme (Stripe dashboard).
 *  - Configurer les conditions de service et fournir la couverture juridique
 *    (RGPD, DSP2, agrément ACPR si modèle d'agent prestataire de services
 *    de paiement).
 *  - La commission (platform fee) est appliquée à l'encaissement du loyer dans
 *    PaymentService (montant dégressif par plan via PlatformFeeService).
 *
 * Sans clé Stripe configurée, les endpoints retournent un état "NONE" et
 * une URL d'onboarding factice (utile pour tester le flow UI).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeConnectService {

    private final UserRepository userRepository;

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public StripeConnectStatusResponse createOrRefreshOnboarding() {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            log.warn("Stripe API key missing — returning placeholder onboarding URL");
            user.setStripeConnectStatus("NONE");
            userRepository.save(user);
            return new StripeConnectStatusResponse(false, "NONE",
                    frontendUrl + "/billing?stripe-connect=not-configured");
        }

        try {
            String accountId = user.getStripeConnectAccountId();
            if (accountId == null || accountId.isBlank()) {
                AccountCreateParams params = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setEmail(user.getEmail())
                        .setCountry("FR")
                        .setCapabilities(AccountCreateParams.Capabilities.builder()
                                .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                .build())
                        .build();
                Account account = Account.create(params);
                accountId = account.getId();
                user.setStripeConnectAccountId(accountId);
                user.setStripeConnectStatus("PENDING");
                userRepository.save(user);
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(frontendUrl + "/billing?stripe-connect=refresh")
                    .setReturnUrl(frontendUrl + "/billing?stripe-connect=return")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            AccountLink link = AccountLink.create(linkParams);

            return new StripeConnectStatusResponse(true, user.getStripeConnectStatus(), link.getUrl());
        } catch (StripeException e) {
            log.error("Stripe Connect onboarding failed for {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Stripe Connect onboarding failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public StripeConnectStatusResponse getStatus() {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        String accountId = user.getStripeConnectAccountId();
        if (accountId != null && !accountId.isBlank() && stripeApiKey != null && !stripeApiKey.isBlank()) {
            try {
                Account account = Account.retrieve(accountId);
                boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();
                boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();

                if (chargesEnabled && payoutsEnabled) {
                    user.setStripeConnectStatus("COMPLETED");
                } else if ("PENDING".equals(user.getStripeConnectStatus())) {
                    user.setStripeConnectStatus("PENDING");
                }
                userRepository.save(user);
            } catch (StripeException e) {
                log.error("Failed to retrieve Stripe Connect account status for {}: {}", accountId, e.getMessage());
            }
        }

        return new StripeConnectStatusResponse(
                accountId != null,
                user.getStripeConnectStatus() != null ? user.getStripeConnectStatus() : "NONE",
                null
        );
    }
}
