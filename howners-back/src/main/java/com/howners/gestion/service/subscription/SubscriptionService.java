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
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ApplicationEventPublisher eventPublisher;

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
                                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 2, 3,
                                    new java.math.BigDecimal("2.5"), null));
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

        if (!isStripeConfigured()) {
            // Mode dev sans Stripe : bascule directe de plan (le front gère sessionId === 'dev-mode')
            log.warn("Stripe non configuré — bascule directe vers le plan {} (mode dev)", plan.getName());
            switchPlanDirectly(userId, user, plan, request.billingPeriod());
            return new CheckoutSessionResponse("dev-mode",
                    frontendUrl + "/billing/success?session_id=dev-mode");
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

    void switchPlanDirectly(UUID userId, User user, SubscriptionPlan newPlan, String billingPeriod) {
        // Première activation payante = aucun abonnement payant géré jusque-là
        boolean premiereActivation = newPlan.getName() != PlanName.FREE
                && subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                        .map(s -> s.getPlan() == null || s.getPlan().getName() == PlanName.FREE)
                        .orElse(true);

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

        if (newPlan.getName() != PlanName.FREE) {
            eventPublisher.publishEvent(new AbonnementActiveEvent(userId, newPlan.getName(), premiereActivation));
        }
    }

    /**
     * Récompense de parrainage : 1 mois de plan PRO offert.
     * - Utilisateur en FREE (ou sans abonnement) : bascule sur PRO pour 1 mois.
     * - Abonnement payant non géré par Stripe : prolonge la période d'1 mois.
     * - Abonnement Stripe actif : applique un coupon 100 % sur la prochaine échéance
     *   (la facturation réelle reste pilotée par Stripe ; la période locale est
     *   resynchronisée par le webhook customer.subscription.updated).
     * Retourne false si la récompense n'a pas pu être appliquée.
     */
    @Transactional
    public boolean offrirMoisPro(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSubscription current = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (current != null && current.getStripeSubscriptionId() != null) {
            return appliquerCouponMoisOffert(current, userId);
        }

        if (current != null && current.getPlan() != null && current.getPlan().getName() != PlanName.FREE) {
            current.setCurrentPeriodEnd(
                    (current.getCurrentPeriodEnd() != null ? current.getCurrentPeriodEnd() : LocalDateTime.now())
                            .plusMonths(1));
            current.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(current);
            log.info("Récompense parrainage : +1 mois sur le plan {} pour {}", current.getPlan().getName(), userId);
            return true;
        }

        SubscriptionPlan proPlan = planRepository.findByName(PlanName.PRO)
                .orElseThrow(() -> new ResourceNotFoundException("Plan PRO introuvable"));

        if (current != null) {
            current.setStatus(SubscriptionStatus.CANCELLED);
            current.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(current);
        }

        UserSubscription reward = UserSubscription.builder()
                .user(user)
                .plan(proPlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(true)
                .build();
        subscriptionRepository.save(reward);
        log.info("Récompense parrainage : 1 mois PRO offert à {}", userId);
        return true;
    }

    /**
     * Applique un coupon Stripe 100 % à usage unique sur la prochaine échéance de
     * l'abonnement : le prochain prélèvement est offert (= 1 mois gratuit).
     */
    private boolean appliquerCouponMoisOffert(UserSubscription current, UUID userId) {
        if (!isStripeConfigured()) {
            log.warn("Récompense parrainage non appliquée pour {} : Stripe non configuré", userId);
            return false;
        }
        try {
            Coupon coupon = Coupon.create(CouponCreateParams.builder()
                    .setPercentOff(new BigDecimal("100"))
                    .setDuration(CouponCreateParams.Duration.ONCE)
                    .setName("Parrainage - 1 mois offert")
                    .setMaxRedemptions(1L)
                    .build());

            Subscription stripeSub = Subscription.retrieve(current.getStripeSubscriptionId());
            stripeSub.update(SubscriptionUpdateParams.builder()
                    .addDiscount(SubscriptionUpdateParams.Discount.builder()
                            .setCoupon(coupon.getId())
                            .build())
                    .build());

            log.info("Récompense parrainage : coupon 100 % (1 mois) appliqué à l'abonnement Stripe {} de {}",
                    current.getStripeSubscriptionId(), userId);
            return true;
        } catch (StripeException e) {
            log.error("Échec de l'application du coupon parrainage Stripe pour {} : {}", userId, e.getMessage());
            return false;
        }
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
                                            String priceId, Long periodStart, Long periodEnd) {
        switch (eventType) {
            case "customer.subscription.created",
                 "customer.subscription.updated" -> {
                handleSubscriptionUpdate(subscriptionId, customerId, priceId, periodStart, periodEnd);
            }
            case "customer.subscription.deleted" -> {
                handleSubscriptionCancelled(subscriptionId);
            }
            default -> log.info("Unhandled subscription event: {}", eventType);
        }
    }

    private void handleSubscriptionUpdate(String subscriptionId, String customerId,
                                           String priceId, Long periodStart, Long periodEnd) {
        UserSubscription sub = subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .or(() -> subscriptionRepository.findByStripeCustomerId(customerId))
                .orElse(null);

        if (sub == null) {
            log.warn("No subscription found for Stripe subscription {}", subscriptionId);
            return;
        }

        // Première activation payante : l'entité n'était pas encore rattachée à un abonnement Stripe
        boolean premiereActivation = sub.getStripeSubscriptionId() == null;

        // Résoudre le plan depuis le price Stripe (le checkout ne le persiste pas)
        if (priceId != null) {
            planRepository.findByStripePriceIdMonthlyOrStripePriceIdAnnual(priceId, priceId)
                    .ifPresent(sub::setPlan);
        }

        premiereActivation = premiereActivation
                && sub.getPlan() != null && sub.getPlan().getName() != PlanName.FREE;

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

        if (sub.getPlan() != null && sub.getPlan().getName() != PlanName.FREE) {
            eventPublisher.publishEvent(new AbonnementActiveEvent(
                    sub.getUser().getId(), sub.getPlan().getName(), premiereActivation));
        }
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

        // Persister le customerId pour que le webhook puisse retrouver l'abonnement
        UserSubscription subToTag = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElse(existingSub);
        if (subToTag == null) {
            assignFreePlan(userId);
            subToTag = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).orElse(null);
        }
        if (subToTag != null) {
            subToTag.setStripeCustomerId(customer.getId());
            subscriptionRepository.save(subToTag);
        }

        return customer.getId();
    }
}
