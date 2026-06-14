import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BienDeclaration {
  propertyId: string;
  nom: string;
  adresse: string;
  revenusBruts: number;
  chargesParLigne: { [ligne: string]: number };
  totalCharges: number;
  revenuNet: number;
}

export interface Declaration2044 {
  annee: number;
  biens: BienDeclaration[];
  totauxParLigne: { [ligne: string]: number };
  totalRevenusBruts: number;
  totalChargesDeductibles: number;
  revenuFoncierNet: number;
}

export const LIGNES_2044_LABELS: { [ligne: string]: string } = {
  '221': "Frais d'administration et de gestion",
  '222': "Primes d'assurance",
  '224': "Réparations, entretien et amélioration",
  '227': 'Taxes foncières et taxes annexes',
  '229': 'Provisions pour charges de copropriété'
};

@Injectable({ providedIn: 'root' })
export class ExportFiscalService {
  private apiUrl = `${environment.apiUrl}/export`;

  constructor(private http: HttpClient) {}

  getApercu(annee: number): Observable<Declaration2044> {
    return this.http.get<Declaration2044>(`${this.apiUrl}/fiscal-2044/apercu?annee=${annee}`);
  }

  download(annee: number, format: 'pdf' | 'csv'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/fiscal-2044?annee=${annee}&format=${format}`,
      { responseType: 'blob' });
  }
}
