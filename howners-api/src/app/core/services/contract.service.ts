import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Contract,
  ContractVersion,
  CreateContractRequest,
  UpdateContractRequest
} from '../models/contract.model';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private apiUrl = `${environment.apiUrl}/contracts`;

  constructor(private http: HttpClient) {}

  /**
   * Créer un nouveau contrat
   */
  createContract(request: CreateContractRequest): Observable<Contract> {
    return this.http.post<Contract>(this.apiUrl, request);
  }

  /**
   * Récupérer tous les contrats du propriétaire
   */
  getMyContracts(): Observable<Contract[]> {
    return this.http.get<Contract[]>(this.apiUrl);
  }

  /**
   * Récupérer un contrat par ID
   */
  getContract(id: string): Observable<Contract> {
    return this.http.get<Contract>(`${this.apiUrl}/${id}`);
  }

  /**
   * Mettre à jour un contrat
   */
  updateContract(id: string, request: UpdateContractRequest): Observable<Contract> {
    return this.http.put<Contract>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Supprimer un contrat
   */
  deleteContract(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les contrats d'une location
   */
  getContractsByRental(rentalId: string): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.apiUrl}/rental/${rentalId}`);
  }

  /**
   * Récupérer toutes les versions d'un contrat
   */
  getContractVersions(id: string): Observable<ContractVersion[]> {
    return this.http.get<ContractVersion[]>(`${this.apiUrl}/${id}/versions`);
  }

  /**
   * Télécharger le PDF du contrat via le backend
   */
  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}
