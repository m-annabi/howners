import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FiscalActivity {
  id: string;
  jurisdiction: string;
  regime: string;
  startDate: string;
  openingCash: number | null;
  siret: string | null;
  active: boolean;
}

export interface AmortizableAsset {
  id: string;
  type: string;
  typeLabel: string;
  label: string;
  base: number;
  startDate: string;
  durationYears: number;
  propertyId: string | null;
}

export interface AssetSuggestion {
  sourceType: string;
  sourceId: string;
  type: string;
  typeLabel: string;
  label: string;
  base: number;
  startDate: string;
  durationYears: number;
}

export interface AmortLine {
  immobilisation: string;
  base: number;
  annuite: number;
  cumul: number;
  vnc: number;
}

export interface Loan {
  id: string;
  label: string;
  principal: number;
  annualRate: number;
  durationMonths: number;
  startDate: string;
  insuranceMonthly: number | null;
  propertyId: string | null;
}

export interface LoanYear {
  year: number;
  interest: number;
  capital: number;
  insurance: number;
  crdEnd: number;
}

export interface ReadinessCheck {
  level: 'DONE' | 'ACTION' | 'INFO';
  titre: string;
  detail: string;
}

export interface ReportLine {
  libelle: string;
  montant: number;
  destination: string;
}

export interface LmnpResult {
  year: number;
  recettes: number;
  chargesParPoste: { [poste: string]: number };
  totalCharges: number;
  resultatAvantAmortissement: number;
  dotationComptable: number;
  amortissementDeductible: number;
  amortissementDiffereCumul: number;
  resultatComptable: number;
  resultatFiscal: number;
  deficitAnterieurImpute: number;
  deficitReportable: number;
  vncImmobilisations: number;
  tresorerie: number;
  capitalExploitant: number;
  reportANouveau: number;
  dettesEmprunt: number;
  totalActif: number;
  totalPassif: number;
  amortissements: AmortLine[];
  avertissements: string[];
  pretADeposer: boolean;
  checklist: ReadinessCheck[];
  reportLines: ReportLine[];
}

@Injectable({ providedIn: 'root' })
export class AccountingService {
  private readonly base = `${environment.apiUrl}/accounting`;

  constructor(private http: HttpClient) {}

  getActivity(): Observable<FiscalActivity | null> {
    return this.http.get<FiscalActivity | null>(`${this.base}/activity`);
  }

  configureActivity(body: { startDate: string; openingCash?: number; siret?: string }): Observable<FiscalActivity> {
    return this.http.post<FiscalActivity>(`${this.base}/activity`, body);
  }

  listAssets(): Observable<AmortizableAsset[]> {
    return this.http.get<AmortizableAsset[]>(`${this.base}/assets`);
  }

  addAsset(body: { type: string; label: string; base: number; startDate: string; durationYears?: number; propertyId?: string }): Observable<AmortizableAsset> {
    return this.http.post<AmortizableAsset>(`${this.base}/assets`, body);
  }

  deleteAsset(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/assets/${id}`);
  }

  suggestions(): Observable<AssetSuggestion[]> {
    return this.http.get<AssetSuggestion[]>(`${this.base}/assets/suggestions`);
  }

  importSuggestions(items: { sourceType: string; sourceId: string; durationYears?: number }[]): Observable<AmortizableAsset[]> {
    return this.http.post<AmortizableAsset[]>(`${this.base}/assets/import`, { items });
  }

  listLoans(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.base}/loans`);
  }

  addLoan(body: { label?: string; principal: number; annualRate: number; durationMonths: number; startDate: string; insuranceMonthly?: number; propertyId?: string }): Observable<Loan> {
    return this.http.post<Loan>(`${this.base}/loans`, body);
  }

  deleteLoan(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/loans/${id}`);
  }

  loanSchedule(id: string): Observable<LoanYear[]> {
    return this.http.get<LoanYear[]>(`${this.base}/loans/${id}/schedule`);
  }

  result(year: number): Observable<LmnpResult> {
    return this.http.get<LmnpResult>(`${this.base}/result?year=${year}`);
  }

  liasseUrl(year: number): string {
    return `${this.base}/liasse?year=${year}`;
  }

  downloadLiasse(year: number): Observable<Blob> {
    return this.http.get(`${this.base}/liasse?year=${year}`, { responseType: 'blob' });
  }
}
