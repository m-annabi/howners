import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContractPublicView, SigningRedirectResponse } from '../models/esignature.model';

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
   * Obtient l'URL de redirection vers DocuSign pour signer
   */
  getSigningRedirect(token: string, returnUrl?: string): Observable<SigningRedirectResponse> {
    let params = new HttpParams();
    if (returnUrl) {
      params = params.set('returnUrl', returnUrl);
    }

    return this.http.post<SigningRedirectResponse>(
      `${this.apiUrl}/token/${token}/redirect`,
      {},
      { params }
    );
  }
}
