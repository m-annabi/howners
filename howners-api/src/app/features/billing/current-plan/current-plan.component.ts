import { Component, OnInit } from '@angular/core';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { UserSubscription, UsageLimits, PLAN_COLORS } from '../../../core/models/subscription.model';

@Component({
  selector: 'app-current-plan',
  templateUrl: './current-plan.component.html'
})
export class CurrentPlanComponent implements OnInit {
  subscription: UserSubscription | null = null;
  usage: UsageLimits | null = null;
  loading = true;
  cancelling = false;

  planColors = PLAN_COLORS;

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.subscriptionService.getCurrentSubscription().subscribe({
      next: (sub) => {
        this.subscription = sub;
        this.loadUsage();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadUsage(): void {
    this.subscriptionService.getUsageLimits().subscribe({
      next: (usage) => {
        this.usage = usage;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getPropertyPercent(): number {
    if (!this.usage || this.usage.maxProperties === -1) return 0;
    return (this.usage.currentProperties / this.usage.maxProperties) * 100;
  }

  getContractPercent(): number {
    if (!this.usage || this.usage.maxContractsPerMonth === -1) return 0;
    return (this.usage.currentContractsThisMonth / this.usage.maxContractsPerMonth) * 100;
  }

  manageBilling(): void {
    this.subscriptionService.createBillingPortal().subscribe({
      next: (response) => {
        window.location.href = response.url;
      }
    });
  }

  cancelSubscription(): void {
    if (confirm('Annuler votre abonnement ? Vous conserverez l\'accès jusqu\'à la fin de la période en cours.')) {
      this.cancelling = true;
      this.subscriptionService.cancelSubscription().subscribe({
        next: () => {
          this.cancelling = false;
          this.loadData();
        },
        error: () => this.cancelling = false
      });
    }
  }

  formatLimit(value: number): string {
    return value === -1 ? 'Illimité' : value.toString();
  }
}
