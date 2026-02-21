import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSearchResult } from '../models/tenant-search-result.model';

export interface TenantDiscoveryFilters {
  city?: string;
  department?: string;
  postalCode?: string;
  budgetMin?: number;
  budgetMax?: number;
  propertyType?: string;
  listingId?: string;
  sortBy?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TenantDiscoveryService {
  private apiUrl = `${environment.apiUrl}/tenant-discovery`;

  constructor(private http: HttpClient) {}

  searchTenants(filters?: TenantDiscoveryFilters): Observable<TenantSearchResult[]> {
    let params = new HttpParams();
    if (filters?.city) params = params.set('city', filters.city);
    if (filters?.department) params = params.set('department', filters.department);
    if (filters?.postalCode) params = params.set('postalCode', filters.postalCode);
    if (filters?.budgetMin != null) params = params.set('budgetMin', filters.budgetMin.toString());
    if (filters?.budgetMax != null) params = params.set('budgetMax', filters.budgetMax.toString());
    if (filters?.propertyType) params = params.set('propertyType', filters.propertyType);
    if (filters?.listingId) params = params.set('listingId', filters.listingId);
    if (filters?.sortBy) params = params.set('sortBy', filters.sortBy);
    return this.http.get<TenantSearchResult[]>(this.apiUrl, { params });
  }

  getTenantProfile(profileId: string, listingId?: string): Observable<TenantSearchResult> {
    let params = new HttpParams();
    if (listingId) params = params.set('listingId', listingId);
    return this.http.get<TenantSearchResult>(`${this.apiUrl}/${profileId}`, { params });
  }
}
