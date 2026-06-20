import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rental } from '../models/rental.model';
import { User } from '../models/user.model';

export interface TenantContract {
  id: string;
  rentalId: string;
  status: string;
  startDate?: string;
  endDate?: string;
  monthlyRent: number;
  currency: string;
  createdAt: string;
}

export interface TenantDocument {
  id: string;
  name: string;
  type: string;
  createdAt: string;
  fileUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly API = `${environment.apiUrl}/tenants`;

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<User> {
    return this.http.get<User>(`${this.API}/me`);
  }

  getMyRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(`${this.API}/me/rentals`);
  }

  getMyContracts(): Observable<TenantContract[]> {
    return this.http.get<TenantContract[]>(`${this.API}/me/contracts`);
  }

  getMyDocuments(): Observable<TenantDocument[]> {
    return this.http.get<TenantDocument[]>(`${this.API}/me/documents`);
  }
}
