import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { Rental } from '../models/rental.model';
import { Contract } from '../models/contract.model';
import { Document } from '../models/document.model';

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private apiUrl = `${environment.apiUrl}/tenants`;

  constructor(private http: HttpClient) {}

  /**
   * Récupérer le profil du locataire connecté
   */
  getMyProfile(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  /**
   * Mettre à jour le profil du locataire
   */
  updateMyProfile(request: UpdateProfileRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/me`, request);
  }

  /**
   * Récupérer mes locations
   */
  getMyRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(`${this.apiUrl}/me/rentals`);
  }

  /**
   * Récupérer mes contrats
   */
  getMyContracts(): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.apiUrl}/me/contracts`);
  }

  /**
   * Récupérer mes documents
   */
  getMyDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/me/documents`);
  }
}
