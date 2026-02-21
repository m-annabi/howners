import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EtatDesLieux, CreateEtatDesLieuxRequest } from '../models/etat-des-lieux.model';

@Injectable({
  providedIn: 'root'
})
export class EtatDesLieuxService {
  private apiUrl = `${environment.apiUrl}/rentals`;

  constructor(private http: HttpClient) {}

  getByRental(rentalId: string): Observable<EtatDesLieux[]> {
    return this.http.get<EtatDesLieux[]>(`${this.apiUrl}/${rentalId}/etat-des-lieux`);
  }

  getById(rentalId: string, id: string): Observable<EtatDesLieux> {
    return this.http.get<EtatDesLieux>(`${this.apiUrl}/${rentalId}/etat-des-lieux/${id}`);
  }

  create(rentalId: string, request: CreateEtatDesLieuxRequest): Observable<EtatDesLieux> {
    return this.http.post<EtatDesLieux>(`${this.apiUrl}/${rentalId}/etat-des-lieux`, request);
  }

  sign(rentalId: string, id: string, role: string): Observable<EtatDesLieux> {
    return this.http.put<EtatDesLieux>(`${this.apiUrl}/${rentalId}/etat-des-lieux/${id}/sign?role=${role}`, {});
  }

  downloadPdf(rentalId: string, id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${rentalId}/etat-des-lieux/${id}/pdf`, {
      responseType: 'blob'
    });
  }

  getMyEdls(): Observable<EtatDesLieux[]> {
    return this.http.get<EtatDesLieux[]>(`${environment.apiUrl}/etat-des-lieux`);
  }
}
