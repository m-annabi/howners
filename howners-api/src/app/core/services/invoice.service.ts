import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Invoice, CreateInvoiceRequest } from '../models/invoice.model';

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {
  private apiUrl = `${environment.apiUrl}/invoices`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(this.apiUrl);
  }

  getById(id: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/${id}`);
  }

  getByRental(rentalId: string): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/rental/${rentalId}`);
  }

  create(request: CreateInvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>(this.apiUrl, request);
  }

  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}
