import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSearchProfile, CreateTenantSearchProfileRequest } from '../models/tenant-search-profile.model';

@Injectable({
  providedIn: 'root'
})
export class TenantSearchProfileService {
  private apiUrl = `${environment.apiUrl}/tenant-search-profile`;

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<TenantSearchProfile> {
    return this.http.get<TenantSearchProfile>(`${this.apiUrl}/me`);
  }

  createOrUpdate(request: CreateTenantSearchProfileRequest): Observable<TenantSearchProfile> {
    return this.http.put<TenantSearchProfile>(`${this.apiUrl}/me`, request);
  }

  activate(): Observable<TenantSearchProfile> {
    return this.http.put<TenantSearchProfile>(`${this.apiUrl}/me/activate`, {});
  }

  deactivate(): Observable<TenantSearchProfile> {
    return this.http.put<TenantSearchProfile>(`${this.apiUrl}/me/deactivate`, {});
  }
}
