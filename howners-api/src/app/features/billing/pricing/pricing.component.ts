import { Component, OnInit } from '@angular/core';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { SubscriptionPlan, PlanName, PLAN_FEATURES, PLAN_COLORS } from '../../../core/models/subscription.model';

@Component({
  selector: 'app-pricing',
  templateUrl: './pricing.component.html',
  styleUrls: ['./pricing.component.scss']
})
export class PricingComponent implements OnInit {
  plans: SubscriptionPlan[] = [];
  loading = false;
  billingPeriod: 'monthly' | 'annual' = 'monthly';
  checkingOut = false;

  planFeatures = PLAN_FEATURES;
  planColors = PLAN_COLORS;

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.loading = true;
    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        this.plans = plans;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getPrice(plan: SubscriptionPlan): number {
    return this.billingPeriod === 'annual'
      ? plan.annualPrice / 12
      : plan.monthlyPrice;
  }

  getTotalPrice(plan: SubscriptionPlan): number {
    return this.billingPeriod === 'annual' ? plan.annualPrice : plan.monthlyPrice;
  }

  checkout(plan: SubscriptionPlan): void {
    if (plan.name === PlanName.FREE) return;

    this.checkingOut = true;
    this.subscriptionService.createCheckout(plan.id, this.billingPeriod).subscribe({
      next: (response) => {
        window.location.href = response.checkoutUrl;
      },
      error: () => this.checkingOut = false
    });
  }

  isPopular(plan: SubscriptionPlan): boolean {
    return plan.name === PlanName.PRO;
  }
}
