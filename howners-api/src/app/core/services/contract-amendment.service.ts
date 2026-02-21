import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContractAmendment, CreateAmendmentRequest } from '../models/contract-amendment.model';

@Injectable({
  providedIn: 'root'
})
export class ContractAmendmentService {
  private apiUrl = `${environment.apiUrl}/contracts`;

  constructor(private http: HttpClient) {}

  getByContract(contractId: string): Observable<ContractAmendment[]> {
    return this.http.get<ContractAmendment[]>(`${this.apiUrl}/${contractId}/amendments`);
  }

  getById(contractId: string, id: string): Observable<ContractAmendment> {
    return this.http.get<ContractAmendment>(`${this.apiUrl}/${contractId}/amendments/${id}`);
  }

  create(contractId: string, request: CreateAmendmentRequest): Observable<ContractAmendment> {
    return this.http.post<ContractAmendment>(`${this.apiUrl}/${contractId}/amendments`, request);
  }

  sign(contractId: string, id: string): Observable<ContractAmendment> {
    return this.http.put<ContractAmendment>(`${this.apiUrl}/${contractId}/amendments/${id}/sign`, {});
  }

  downloadPdf(contractId: string, id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${contractId}/amendments/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}
