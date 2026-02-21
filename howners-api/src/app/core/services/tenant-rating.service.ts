import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TenantRating,
  TenantRatingSummary,
  CreateTenantRatingRequest,
  UpdateTenantRatingRequest
} from '../models/tenant-rating.model';

@Injectable({
  providedIn: 'root'
})
export class TenantRatingService {
  private readonly API_URL = `${environment.apiUrl}/ratings`;

  constructor(private http: HttpClient) {}

  getRatingsByTenant(tenantId: string): Observable<TenantRating[]> {
    return this.http.get<TenantRating[]>(`${this.API_URL}/tenant/${tenantId}`);
  }

  getTenantSummary(tenantId: string): Observable<TenantRatingSummary> {
    return this.http.get<TenantRatingSummary>(`${this.API_URL}/tenant/${tenantId}/summary`);
  }

  getRatingsByRental(rentalId: string): Observable<TenantRating[]> {
    return this.http.get<TenantRating[]>(`${this.API_URL}/rental/${rentalId}`);
  }

  getMyRatings(): Observable<TenantRating[]> {
    return this.http.get<TenantRating[]>(`${this.API_URL}/mine`);
  }

  getRating(id: string): Observable<TenantRating> {
    return this.http.get<TenantRating>(`${this.API_URL}/${id}`);
  }

  createRating(request: CreateTenantRatingRequest): Observable<TenantRating> {
    return this.http.post<TenantRating>(this.API_URL, request);
  }

  updateRating(id: string, request: UpdateTenantRatingRequest): Observable<TenantRating> {
    return this.http.put<TenantRating>(`${this.API_URL}/${id}`, request);
  }

  deleteRating(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
