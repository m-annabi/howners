import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface IrlIndice {
  id: string;
  annee: number;
  trimestre: number;
  valeur: number;
}

export interface RevisionLoyer {
  id: string;
  rentalId: string;
  propertyName: string | null;
  ancienLoyer: number;
  nouveauLoyer: number;
  indiceAncien: IrlIndice | null;
  indiceNouveau: IrlIndice | null;
  dateRevision: string;
  dateEffet: string | null;
  statut: 'BROUILLON' | 'NOTIFIEE' | 'APPLIQUEE' | 'ANNULEE';
  documentId: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class RentRevisionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getIndices(): Observable<IrlIndice[]> {
    return this.http.get<IrlIndice[]>(`${this.apiUrl}/irl-indices`);
  }

  addIndice(indice: { annee: number; trimestre: number; valeur: number }): Observable<IrlIndice> {
    return this.http.post<IrlIndice>(`${this.apiUrl}/irl-indices`, indice);
  }

  getRevisions(rentalId: string): Observable<RevisionLoyer[]> {
    return this.http.get<RevisionLoyer[]>(`${this.apiUrl}/rentals/${rentalId}/revisions`);
  }

  calculer(rentalId: string): Observable<RevisionLoyer> {
    return this.http.post<RevisionLoyer>(`${this.apiUrl}/rentals/${rentalId}/revisions/calculer`, {});
  }

  notifier(revisionId: string): Observable<RevisionLoyer> {
    return this.http.post<RevisionLoyer>(`${this.apiUrl}/revisions/${revisionId}/notifier`, {});
  }

  appliquer(revisionId: string): Observable<RevisionLoyer> {
    return this.http.post<RevisionLoyer>(`${this.apiUrl}/revisions/${revisionId}/appliquer`, {});
  }

  annuler(revisionId: string): Observable<RevisionLoyer> {
    return this.http.post<RevisionLoyer>(`${this.apiUrl}/revisions/${revisionId}/annuler`, {});
  }

  downloadCourrier(revisionId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/revisions/${revisionId}/courrier`, { responseType: 'blob' });
  }
}
