import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
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
  error: string | null = null;
  billingPeriod: 'monthly' | 'annual' = 'monthly';
  checkingOut = false;

  planFeatures = PLAN_FEATURES;
  planColors = PLAN_COLORS;

  constructor(
    private subscriptionService: SubscriptionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.error = null;
    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        this.plans = plans;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des plans';
        this.loading = false;
      }
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
        if (response.sessionId === 'dev-mode') {
          this.router.navigate(['/billing/success'], { queryParams: { session_id: 'dev-mode' } });
        } else {
          window.location.href = response.checkoutUrl;
        }
      },
      error: () => this.checkingOut = false
    });
  }

  isPopular(plan: SubscriptionPlan): boolean {
    return plan.name === PlanName.PRO;
  }
}
