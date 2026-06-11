import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Listing, CreateListingRequest } from '../models/listing.model';
import { Page } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class ListingService {
  private apiUrl = `${environment.apiUrl}/listings`;

  constructor(private http: HttpClient) {}

  searchListings(filters?: {
    search?: string; city?: string; department?: string; postalCode?: string;
    priceMin?: number; priceMax?: number; propertyType?: string;
    minSurface?: number; minBedrooms?: number; furnished?: boolean;
    availableFrom?: string; sortBy?: string;
    nearLat?: number; nearLng?: number; radiusKm?: number;
    page?: number; size?: number;
  }): Observable<Page<Listing>> {
    let params = new HttpParams();
    if (filters?.search) params = params.set('search', filters.search);
    if (filters?.city) params = params.set('city', filters.city);
    if (filters?.department) params = params.set('department', filters.department);
    if (filters?.postalCode) params = params.set('postalCode', filters.postalCode);
    if (filters?.priceMin != null) params = params.set('priceMin', filters.priceMin.toString());
    if (filters?.priceMax != null) params = params.set('priceMax', filters.priceMax.toString());
    if (filters?.propertyType) params = params.set('propertyType', filters.propertyType);
    if (filters?.minSurface != null) params = params.set('minSurface', filters.minSurface.toString());
    if (filters?.minBedrooms != null) params = params.set('minBedrooms', filters.minBedrooms.toString());
    if (filters?.furnished != null) params = params.set('furnished', filters.furnished.toString());
    if (filters?.availableFrom) params = params.set('availableFrom', filters.availableFrom);
    if (filters?.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters?.nearLat != null) params = params.set('nearLat', filters.nearLat.toString());
    if (filters?.nearLng != null) params = params.set('nearLng', filters.nearLng.toString());
    if (filters?.radiusKm != null) params = params.set('radiusKm', filters.radiusKm.toString());
    if (filters?.page != null) params = params.set('page', filters.page.toString());
    if (filters?.size != null) params = params.set('size', filters.size.toString());
    return this.http.get<Page<Listing>>(this.apiUrl, { params });
  }

  getListing(id: string): Observable<Listing> {
    return this.http.get<Listing>(`${this.apiUrl}/${id}`);
  }

  getMyListings(page?: number, size?: number): Observable<Page<Listing>> {
    let params = new HttpParams();
    if (page != null) params = params.set('page', page.toString());
    if (size != null) params = params.set('size', size.toString());
    return this.http.get<Page<Listing>>(`${this.apiUrl}/my`, { params });
  }

  createListing(request: CreateListingRequest): Observable<Listing> {
    return this.http.post<Listing>(this.apiUrl, request);
  }

  updateListing(id: string, request: CreateListingRequest): Observable<Listing> {
    return this.http.put<Listing>(`${this.apiUrl}/${id}`, request);
  }

  publishListing(id: string): Observable<Listing> {
    return this.http.put<Listing>(`${this.apiUrl}/${id}/publish`, {});
  }

  pauseListing(id: string): Observable<Listing> {
    return this.http.put<Listing>(`${this.apiUrl}/${id}/pause`, {});
  }

  closeListing(id: string): Observable<Listing> {
    return this.http.put<Listing>(`${this.apiUrl}/${id}/close`, {});
  }

  deleteListing(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
