import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Regularisation {
  id: string;
  rentalId: string;
  propertyName: string | null;
  annee: number;
  provisionsEncaissees: number;
  chargesReelles: number;
  solde: number;
  detail: { [categorie: string]: number } | null;
  statut: 'BROUILLON' | 'ENVOYEE' | 'SOLDEE';
  documentId: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ChargeRegularisationService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getRegularisations(rentalId: string): Observable<Regularisation[]> {
    return this.http.get<Regularisation[]>(`${this.apiUrl}/rentals/${rentalId}/regularisations`);
  }

  calculer(rentalId: string, annee: number): Observable<Regularisation> {
    return this.http.post<Regularisation>(
      `${this.apiUrl}/rentals/${rentalId}/regularisations?annee=${annee}`, {});
  }

  envoyer(id: string): Observable<Regularisation> {
    return this.http.post<Regularisation>(`${this.apiUrl}/regularisations/${id}/envoyer`, {});
  }

  creerPaiement(id: string): Observable<Regularisation> {
    return this.http.post<Regularisation>(`${this.apiUrl}/regularisations/${id}/paiement`, {});
  }

  downloadDecompte(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/regularisations/${id}/decompte`, { responseType: 'blob' });
  }
}
