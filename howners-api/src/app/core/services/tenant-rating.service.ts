import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantRating, CreateTenantRatingRequest } from '../models/tenant-rating.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class TenantRatingService {
  private api = `${environment.apiUrl}/tenant-ratings`;

  constructor(private http: HttpClient) {}

  create(request: CreateTenantRatingRequest): Observable<TenantRating> {
    return this.http.post<TenantRating>(this.api, request);
  }

  getRatingsForTenant(tenantId: string): Observable<TenantRating[]> {
    return this.http.get<TenantRating[]>(`${this.api}/tenant/${tenantId}`);
  }

  getMyRatings(): Observable<TenantRating[]> {
    return this.http.get<TenantRating[]>(`${this.api}/my`);
  }

  getTenantProfile(tenantId: string): Observable<User> {
    return this.http.get<User>(`${this.api}/tenant/${tenantId}/profile`);
  }
}
