import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StripeConnectStatus {
  connected: boolean;
  status: string;
  onboardingUrl: string | null;
  paymentInstructions: string | null;
  acceptOnlinePayments: boolean;
  /** Jour d'envoi des quittances (1-28) ; null = envoi immédiat à la confirmation du paiement. */
  receiptSendDay: number | null;
  /** Commission plateforme (%) sur les paiements carte. */
  platformFeePercent: number | null;
}

export interface UpdatePaymentSettingsRequest {
  paymentInstructions: string;
  acceptOnlinePayments: boolean;
  receiptSendDay: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class StripeConnectService {
  private apiUrl = `${environment.apiUrl}/stripe-connect`;

  constructor(private http: HttpClient) {}

  getStatus(): Observable<StripeConnectStatus> {
    return this.http.get<StripeConnectStatus>(`${this.apiUrl}/status`);
  }

  /** Démarre ou reprend l'onboarding Stripe Connect ; renvoie l'URL à ouvrir. */
  startOnboarding(): Observable<StripeConnectStatus> {
    return this.http.post<StripeConnectStatus>(`${this.apiUrl}/onboarding`, {});
  }

  updatePaymentSettings(request: UpdatePaymentSettingsRequest): Observable<StripeConnectStatus> {
    return this.http.put<StripeConnectStatus>(`${this.apiUrl}/payment-settings`, request);
  }
}
