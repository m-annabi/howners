package com.howners.gestion.repository;

import com.howners.gestion.domain.subscription.PlanName;
import com.howners.gestion.domain.subscription.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByName(PlanName name);
}
