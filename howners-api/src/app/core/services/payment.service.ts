import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Payment,
  CreatePaymentRequest
} from '../models/payment.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Payment[]> {
    return this.http.get<Payment[]>(this.apiUrl);
  }

  getById(id: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${id}`);
  }

  getByRental(rentalId: string): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.apiUrl}/rental/${rentalId}`);
  }

  create(request: CreatePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, request);
  }

  confirmPayment(paymentId: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${paymentId}/confirm`, {});
  }

  /** Crée une session Stripe Checkout hébergée pour régler le loyer. */
  createCheckout(paymentId: string): Observable<{ sessionId: string; checkoutUrl: string }> {
    return this.http.post<{ sessionId: string; checkoutUrl: string }>(
      `${this.apiUrl}/${paymentId}/checkout`, {}
    );
  }

  /** Finalise le paiement au retour de Stripe (vérifie la session). */
  finalizeCheckout(paymentId: string, sessionId: string): Observable<Payment> {
    return this.http.post<Payment>(
      `${this.apiUrl}/${paymentId}/checkout/confirm`, null,
      { params: new HttpParams().set('sessionId', sessionId) }
    );
  }

  relancer(paymentId: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${paymentId}/relancer`, {});
  }
}
