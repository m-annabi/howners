import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EtatDesLieux, CreateEtatDesLieuxRequest } from '../models/etat-des-lieux.model';

export interface PieceComparee {
  nom: string;
  etatEntree: string | null;
  etatSortie: string | null;
  commentairesEntree: string | null;
  commentairesSortie: string | null;
  degradee: boolean;
  nonComparable: boolean;
}

export interface CompteurCompare {
  type: string;
  releveEntree: string | null;
  releveSortie: string | null;
}

export interface RetenueDepot {
  piece: string;
  etatEntree: string | null;
  etatSortie: string | null;
  motif: string;
  montant: number;
}

export interface ComparaisonEdl {
  id: string | null;
  rentalId: string;
  edlEntreeId: string;
  edlSortieId: string;
  dateEntree: string | null;
  dateSortie: string | null;
  pieces: PieceComparee[];
  compteurs: CompteurCompare[];
  clesEntree: number | null;
  clesSortie: number | null;
  retenues: RetenueDepot[];
  totalRetenues: number;
  depositAmount: number | null;
  soldeARestituer: number | null;
  statut: 'BROUILLON' | 'VALIDEE';
  documentId: string | null;
}

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

  getComparaison(rentalId: string): Observable<ComparaisonEdl> {
    return this.http.get<ComparaisonEdl>(`${this.apiUrl}/${rentalId}/edl/comparaison`);
  }

  enregistrerRetenues(rentalId: string, retenues: RetenueDepot[]): Observable<ComparaisonEdl> {
    return this.http.put<ComparaisonEdl>(
      `${this.apiUrl}/${rentalId}/edl/comparaison/retenues`, { retenues });
  }

  validerComparaison(rentalId: string): Observable<ComparaisonEdl> {
    return this.http.post<ComparaisonEdl>(`${this.apiUrl}/${rentalId}/edl/comparaison/valider`, {});
  }

  downloadComparaisonPdf(comparaisonId: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/edl-comparaisons/${comparaisonId}/pdf`,
      { responseType: 'blob' });
  }
}
