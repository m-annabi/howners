import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rental, CreateRentalRequest, UpdateRentalRequest, PublishRentalRequest, ExitTenantRequest } from '../../core/models/rental.model';

@Injectable({
  providedIn: 'root'
})
export class RentalService {
  private readonly API_URL = `${environment.apiUrl}/rentals`;

  constructor(private http: HttpClient) {}

  getRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(this.API_URL);
  }

  getRental(id: string): Observable<Rental> {
    return this.http.get<Rental>(`${this.API_URL}/${id}`);
  }

  createRental(request: CreateRentalRequest): Observable<Rental> {
    return this.http.post<Rental>(this.API_URL, request);
  }

  updateRental(id: string, request: UpdateRentalRequest): Observable<Rental> {
    return this.http.put<Rental>(`${this.API_URL}/${id}`, request);
  }

  deleteRental(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  publishRental(id: string, request: PublishRentalRequest): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/${id}/publish`, request);
  }

  exitTenant(id: string, request: ExitTenantRequest): Observable<Rental> {
    return this.http.post<Rental>(`${this.API_URL}/${id}/exit-tenant`, request);
  }

  confirmExit(id: string): Observable<Rental> {
    return this.http.post<Rental>(`${this.API_URL}/${id}/confirm-exit`, {});
  }
}
