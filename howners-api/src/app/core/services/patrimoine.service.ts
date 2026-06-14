import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BienPatrimoine {
  propertyId: string;
  nom: string;
  ville: string | null;
  purchasePrice: number | null;
  revenusAnnuels: number;
  chargesAnnuelles: number;
  cashFlowMensuel: number;
  rentabiliteBrutePercent: number | null;
  rentabiliteNettePercent: number | null;
  tauxOccupationPercent: number;
}

export interface Patrimoine {
  biens: BienPatrimoine[];
  valeurAchatTotale: number;
  revenusAnnuelsTotaux: number;
  chargesAnnuellesTotales: number;
  cashFlowMensuelTotal: number;
  rendementNetMoyenPondere: number | null;
}

@Injectable({ providedIn: 'root' })
export class PatrimoineService {
  private apiUrl = `${environment.apiUrl}/financial`;

  constructor(private http: HttpClient) {}

  getPatrimoine(): Observable<Patrimoine> {
    return this.http.get<Patrimoine>(`${this.apiUrl}/patrimoine`);
  }
}
