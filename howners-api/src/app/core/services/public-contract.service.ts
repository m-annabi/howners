import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContractPublicView } from '../models/esignature.model';

/**
 * Service pour l'accès public aux contrats via token (sans authentification)
 */
@Injectable({
  providedIn: 'root'
})
export class PublicContractService {
  private readonly apiUrl = `${environment.apiUrl}/public/contracts`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère un contrat par son token d'accès
   */
  getContractByToken(token: string): Observable<ContractPublicView> {
    return this.http.get<ContractPublicView>(`${this.apiUrl}/token/${token}`);
  }

  /**
   * Télécharge le PDF du contrat pour prévisualisation
   */
  downloadContractPdf(token: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/token/${token}/pdf`, {
      responseType: 'blob'
    });
  }

  /**
   * Signe le contrat directement avec l'image de signature
   */
  signContract(token: string, signatureData: string, signerName: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/token/${token}/sign`, {
      signatureData,
      signerName
    });
  }
}
