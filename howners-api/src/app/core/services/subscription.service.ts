import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SubscriptionPlan,
  UserSubscription,
  CheckoutSessionResponse,
  UsageLimits
} from '../models/subscription.model';

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl = `${environment.apiUrl}/subscriptions`;

  constructor(private http: HttpClient) {}

  getPlans(): Observable<SubscriptionPlan[]> {
    return this.http.get<SubscriptionPlan[]>(`${this.apiUrl}/plans`);
  }

  getCurrentSubscription(): Observable<UserSubscription> {
    return this.http.get<UserSubscription>(`${this.apiUrl}/current`);
  }

  createCheckout(planId: string, billingPeriod: string): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout`, { planId, billingPeriod });
  }

  createBillingPortal(): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.apiUrl}/billing-portal`, {});
  }

  cancelSubscription(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/cancel`, {});
  }

  getUsageLimits(): Observable<UsageLimits> {
    return this.http.get<UsageLimits>(`${this.apiUrl}/usage`);
  }
}
