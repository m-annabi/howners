import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Signature, CreateSignatureRequest } from '../models/signature.model';

@Injectable({
  providedIn: 'root'
})
export class SignatureService {
  private apiUrl = `${environment.apiUrl}/signatures`;

  constructor(private http: HttpClient) {}

  /**
   * Créer une signature
   */
  createSignature(request: CreateSignatureRequest): Observable<Signature> {
    return this.http.post<Signature>(this.apiUrl, request);
  }

  /**
   * Récupérer toutes les signatures de l'utilisateur
   */
  getMySignatures(): Observable<Signature[]> {
    return this.http.get<Signature[]>(this.apiUrl);
  }

  /**
   * Récupérer une signature par ID
   */
  getSignature(id: string): Observable<Signature> {
    return this.http.get<Signature>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les signatures d'un contrat
   */
  getContractSignatures(contractId: string): Observable<Signature[]> {
    return this.http.get<Signature[]>(`${this.apiUrl}/contract/${contractId}`);
  }
}
