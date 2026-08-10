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

  private readonly taglines: { [key: string]: string } = {
    [PlanName.FREE]: 'Pour démarrer et gérer vos premiers biens.',
    [PlanName.PRO]: 'Pour les bailleurs qui veulent piloter leur patrimoine.',
    [PlanName.PREMIUM]: 'Pour les portefeuilles conséquents, sans limite.',
    [PlanName.AGENCE]: 'Pour les agences et SCI multi-comptes.'
  };

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

  isFree(plan: SubscriptionPlan): boolean {
    return plan.name === PlanName.FREE || plan.monthlyPrice === 0;
  }

  getTagline(plan: SubscriptionPlan): string {
    return this.taglines[plan.name] || '';
  }

  getSavingsPercent(plan: SubscriptionPlan): number | null {
    if (!plan.monthlyPrice || !plan.annualPrice) return null;
    const full = plan.monthlyPrice * 12;
    if (plan.annualPrice >= full) return null;
    return Math.round((1 - plan.annualPrice / full) * 100);
  }
}
