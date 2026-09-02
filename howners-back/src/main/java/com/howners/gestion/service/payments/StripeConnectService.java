package com.howners.gestion.service.payments;

import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.payments.StripeConnectStatusResponse;
import com.howners.gestion.dto.payments.UpdatePaymentSettingsRequest;
import com.howners.gestion.exception.BadRequestException;
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
                    frontendUrl + "/profile?stripe-connect=not-configured",
                    user.getPaymentInstructions(), Boolean.TRUE.equals(user.getAcceptOnlinePayments()));
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
                    .setRefreshUrl(frontendUrl + "/profile?stripe-connect=refresh")
                    .setReturnUrl(frontendUrl + "/profile?stripe-connect=return")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            AccountLink link = AccountLink.create(linkParams);

            return new StripeConnectStatusResponse(true, user.getStripeConnectStatus(), link.getUrl(),
                    user.getPaymentInstructions(), Boolean.TRUE.equals(user.getAcceptOnlinePayments()));
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
                } else {
                    user.setStripeConnectStatus("PENDING");
                    // Le compte n'est plus opérationnel (Stripe redemande des infos, capacité
                    // révoquée…) : on ne laisse pas le paiement carte actif dans ce cas.
                    user.setAcceptOnlinePayments(false);
                }
                userRepository.save(user);
            } catch (StripeException e) {
                log.error("Failed to retrieve Stripe Connect account status for {}: {}", accountId, e.getMessage());
            }
        }

        return new StripeConnectStatusResponse(
                accountId != null,
                user.getStripeConnectStatus() != null ? user.getStripeConnectStatus() : "NONE",
                null,
                user.getPaymentInstructions(),
                Boolean.TRUE.equals(user.getAcceptOnlinePayments())
        );
    }

    /**
     * Met à jour les coordonnées de paiement déclaratives du bailleur et/ou l'activation du
     * paiement carte en ligne. Refuse d'activer si l'onboarding Connect n'est pas complet — le
     * bailleur ne peut pas se retrouver en attente de virement d'une carte qu'il ne peut pas
     * encaisser.
     */
    @Transactional
    public StripeConnectStatusResponse updatePaymentSettings(UpdatePaymentSettingsRequest request) {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        if (Boolean.TRUE.equals(request.acceptOnlinePayments())
                && !"COMPLETED".equals(user.getStripeConnectStatus())) {
            throw new BadRequestException(
                    "Impossible d'activer le paiement en ligne : votre compte Stripe Connect n'est pas encore complété.");
        }

        user.setPaymentInstructions(request.paymentInstructions());
        user.setAcceptOnlinePayments(Boolean.TRUE.equals(request.acceptOnlinePayments()));
        userRepository.save(user);

        return new StripeConnectStatusResponse(
                user.getStripeConnectAccountId() != null,
                user.getStripeConnectStatus() != null ? user.getStripeConnectStatus() : "NONE",
                null,
                user.getPaymentInstructions(),
                user.getAcceptOnlinePayments()
        );
    }

    /**
     * Met à jour le statut Connect d'un bailleur depuis un webhook account.updated
     * (synchro en push, sans attendre que le bailleur consulte sa page).
     */
    @Transactional
    public void processAccountUpdate(String accountId, Boolean chargesEnabled, Boolean payoutsEnabled) {
        if (accountId == null) {
            return;
        }
        userRepository.findByStripeConnectAccountId(accountId).ifPresent(user -> {
            boolean charges = Boolean.TRUE.equals(chargesEnabled);
            boolean payouts = Boolean.TRUE.equals(payoutsEnabled);
            String newStatus = (charges && payouts) ? "COMPLETED" : "PENDING";
            boolean statusChanged = !newStatus.equals(user.getStripeConnectStatus());
            if (statusChanged) {
                user.setStripeConnectStatus(newStatus);
            }
            if (!"COMPLETED".equals(newStatus) && Boolean.TRUE.equals(user.getAcceptOnlinePayments())) {
                user.setAcceptOnlinePayments(false);
                statusChanged = true;
            }
            if (statusChanged) {
                userRepository.save(user);
                log.info("Statut Connect du compte {} mis à jour en {} pour l'utilisateur {}",
                        accountId, newStatus, user.getId());
            }
        });
    }
}
