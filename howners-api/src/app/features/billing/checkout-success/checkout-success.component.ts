import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { PlanName } from '../../../core/models/subscription.model';

type SuccessState = 'checking' | 'activated' | 'pending';

@Component({
  selector: 'app-checkout-success',
  templateUrl: './checkout-success.component.html',
  styleUrls: ['./checkout-success.component.scss']
})
export class CheckoutSuccessComponent implements OnInit, OnDestroy {
  state: SuccessState = 'checking';
  planName = '';

  private attempts = 0;
  private readonly maxAttempts = 5;
  private pollSub?: Subscription;

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.poll();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  // L'activation est asynchrone (webhook Stripe) : on sonde brièvement avant d'afficher l'état.
  private poll(): void {
    this.subscriptionService.getCurrentSubscription().subscribe({
      next: (sub) => {
        if (sub?.plan && sub.plan.name !== PlanName.FREE) {
          this.planName = sub.plan.displayName || sub.plan.name;
          this.state = 'activated';
          return;
        }
        this.scheduleRetry();
      },
      error: () => this.scheduleRetry()
    });
  }

  private scheduleRetry(): void {
    this.attempts += 1;
    if (this.attempts >= this.maxAttempts) {
      this.state = 'pending';
      return;
    }
    this.pollSub = timer(2000).subscribe(() => this.poll());
  }
}
