package com.howners.gestion.service.subscription;

import com.howners.gestion.domain.subscription.*;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.subscription.*;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.SubscriptionPlanRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.repository.UserSubscriptionRepository;
import com.howners.gestion.service.auth.AuthService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    private boolean isStripeConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSubscriptionResponse getCurrentSubscription() {
        UUID userId = AuthService.getCurrentUserId();
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(UserSubscriptionResponse::from)
                .orElseGet(() -> {
                    // Return a virtual FREE plan response
                    SubscriptionPlanResponse freePlan = planRepository.findByName(PlanName.FREE)
                            .map(SubscriptionPlanResponse::from)
                            .orElse(new SubscriptionPlanResponse(null, PlanName.FREE, "Gratuit",
                                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 2, 3, null));
                    return new UserSubscriptionResponse(null, userId, freePlan,
                            SubscriptionStatus.ACTIVE, null, null, false, LocalDateTime.now());
                });
    }

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        UUID userId = AuthService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SubscriptionPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (plan.getName() == PlanName.FREE) {
            throw new BadRequestException("Cannot checkout for free plan");
        }

        // Dev mode: if Stripe is not configured, switch plan directly
        if (!isStripeConfigured()) {
            log.info("Stripe not configured — switching plan directly for user {}", userId);
            switchPlanDirectly(userId, user, plan, request.billingPeriod());
            String successUrl = frontendUrl + "/billing/success?session_id=dev-mode";
            return new CheckoutSessionResponse("dev-mode", successUrl);
        }

        try {
            // Get or create Stripe customer
            String customerId = getOrCreateStripeCustomer(userId, user);

            // Determine price ID
            boolean isAnnual = "annual".equalsIgnoreCase(request.billingPeriod());
            String priceId = isAnnual ? plan.getStripePriceIdAnnual() : plan.getStripePriceIdMonthly();

            if (priceId == null || priceId.isBlank()) {
                throw new BadRequestException("Stripe price not configured for this plan");
            }

            // Create Stripe Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(frontendUrl + "/billing/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/billing")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("userId", userId.toString())
                    .putMetadata("planId", plan.getId().toString())
                    .build();

            Session session = Session.create(params);
            log.info("Checkout session created for user {} plan {}", userId, plan.getName());

            return new CheckoutSessionResponse(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session: {}", e.getMessage());
            throw new RuntimeException("Failed to create checkout session", e);
        }
    }

    private void switchPlanDirectly(UUID userId, User user, SubscriptionPlan newPlan, String billingPeriod) {
        // Deactivate current subscription
        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.CANCELLED);
                    sub.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);
                });

        // Create new subscription
        boolean isAnnual = "annual".equalsIgnoreCase(billingPeriod);
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(newPlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(isAnnual ? now.plusYears(1) : now.plusMonths(1))
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);
        log.info("Plan switched directly to {} for user {}", newPlan.getName(), userId);
    }

    @Transactional
    public String createBillingPortalSession() {
        UUID userId = AuthService.getCurrentUserId();
        UserSubscription sub = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("No active subscription found"));

        if (!isStripeConfigured()) {
            return frontendUrl + "/billing";
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(sub.getStripeCustomerId())
                            .setReturnUrl(frontendUrl + "/billing")
                            .build();

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);

            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe error creating billing portal: {}", e.getMessage());
            throw new RuntimeException("Failed to create billing portal session", e);
        }
    }

    @Transactional
    public void cancelSubscription() {
        UUID userId = AuthService.getCurrentUserId();
        UserSubscription sub = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("No active subscription found"));

        if (isStripeConfigured() && sub.getStripeSubscriptionId() != null) {
            try {
                Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
                stripeSub.update(com.stripe.param.SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build());
            } catch (StripeException e) {
                log.error("Stripe error cancelling subscription: {}", e.getMessage());
                throw new RuntimeException("Failed to cancel subscription", e);
            }
        }

        // In dev mode (no Stripe), cancel immediately and assign FREE plan
        if (!isStripeConfigured()) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
            assignFreePlan(userId);
            log.info("Subscription cancelled (dev mode) for user {}", userId);
            return;
        }

        sub.setCancelAtPeriodEnd(true);
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
        log.info("Subscription cancelled for user {}", userId);
    }

    @Transactional
    public void assignFreePlan(UUID userId) {
        // Check if user already has a subscription
        if (subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }

        SubscriptionPlan freePlan = planRepository.findByName(PlanName.FREE)
                .orElse(null);

        if (freePlan == null) {
            log.warn("FREE plan not found in database, skipping auto-assign");
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        subscriptionRepository.save(subscription);
        log.info("Free plan assigned to user {}", userId);
    }

    @Transactional
    public void processSubscriptionWebhook(String eventType, String subscriptionId, String customerId,
                                            Long periodStart, Long periodEnd) {
        switch (eventType) {
            case "customer.subscription.created",
                 "customer.subscription.updated" -> {
                handleSubscriptionUpdate(subscriptionId, customerId, periodStart, periodEnd);
            }
            case "customer.subscription.deleted" -> {
                handleSubscriptionCancelled(subscriptionId);
            }
            default -> log.info("Unhandled subscription event: {}", eventType);
        }
    }

    private void handleSubscriptionUpdate(String subscriptionId, String customerId,
                                           Long periodStart, Long periodEnd) {
        UserSubscription sub = subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .or(() -> subscriptionRepository.findByStripeCustomerId(customerId))
                .orElse(null);

        if (sub == null) {
            log.warn("No subscription found for Stripe subscription {}", subscriptionId);
            return;
        }

        sub.setStripeSubscriptionId(subscriptionId);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelAtPeriodEnd(false);
        if (periodStart != null) {
            sub.setCurrentPeriodStart(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(periodStart), ZoneId.systemDefault()));
        }
        if (periodEnd != null) {
            sub.setCurrentPeriodEnd(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEnd), ZoneId.systemDefault()));
        }
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
        log.info("Subscription updated: {}", subscriptionId);
    }

    private void handleSubscriptionCancelled(String subscriptionId) {
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.CANCELLED);
                    sub.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);

                    // Auto-assign FREE plan
                    assignFreePlan(sub.getUser().getId());
                    log.info("Subscription cancelled, FREE plan assigned: {}", subscriptionId);
                });
    }

    private String getOrCreateStripeCustomer(UUID userId, User user) throws StripeException {
        // Check if user already has a Stripe customer
        UserSubscription existingSub = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        if (existingSub != null && existingSub.getStripeCustomerId() != null) {
            return existingSub.getStripeCustomerId();
        }

        // Create new Stripe customer
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .putMetadata("userId", userId.toString())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
