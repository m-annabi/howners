import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Payment,
  CreatePaymentRequest,
  StripePaymentIntentResponse
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

  createStripeIntent(paymentId: string): Observable<StripePaymentIntentResponse> {
    return this.http.post<StripePaymentIntentResponse>(
      `${this.apiUrl}/${paymentId}/stripe-intent`, {}
    );
  }

  confirmPayment(paymentId: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${paymentId}/confirm`, {});
  }
}
