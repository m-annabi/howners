import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContractTemplate,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  PreviewTemplateRequest,
  PreviewTemplateResponse,
  TemplateVariablesResponse,
  RentalType
} from '../models/contract-template.model';

@Injectable({
  providedIn: 'root'
})
export class ContractTemplateService {
  private apiUrl = `${environment.apiUrl}/contract-templates`;

  constructor(private http: HttpClient) {}

  /**
   * Récupérer tous les templates accessibles (personnels + par défaut)
   */
  getMyTemplates(rentalType?: RentalType): Observable<ContractTemplate[]> {
    let params = new HttpParams();
    if (rentalType) {
      params = params.set('rentalType', rentalType);
    }
    return this.http.get<ContractTemplate[]>(this.apiUrl, { params });
  }

  /**
   * Récupérer un template par ID
   */
  getTemplate(id: string): Observable<ContractTemplate> {
    return this.http.get<ContractTemplate>(`${this.apiUrl}/${id}`);
  }

  /**
   * Créer un nouveau template
   */
  createTemplate(request: CreateTemplateRequest): Observable<ContractTemplate> {
    return this.http.post<ContractTemplate>(this.apiUrl, request);
  }

  /**
   * Mettre à jour un template
   */
  updateTemplate(id: string, request: UpdateTemplateRequest): Observable<ContractTemplate> {
    return this.http.put<ContractTemplate>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Supprimer un template
   */
  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Dupliquer un template
   */
  duplicateTemplate(id: string, newName: string): Observable<ContractTemplate> {
    const params = new HttpParams().set('newName', newName);
    return this.http.post<ContractTemplate>(`${this.apiUrl}/${id}/duplicate`, null, { params });
  }

  /**
   * Prévisualiser un template rempli avec les données d'une location
   */
  previewTemplate(templateId: string, rentalId: string): Observable<PreviewTemplateResponse> {
    const params = new HttpParams().set('rentalId', rentalId);
    return this.http.post<PreviewTemplateResponse>(`${this.apiUrl}/${templateId}/preview`, null, { params });
  }

  /**
   * Prévisualiser un contenu personnalisé rempli avec les données d'une location
   */
  previewCustomContent(request: PreviewTemplateRequest): Observable<PreviewTemplateResponse> {
    return this.http.post<PreviewTemplateResponse>(`${this.apiUrl}/preview-custom`, request);
  }

  /**
   * Récupérer toutes les variables disponibles
   */
  getAvailableVariables(): Observable<TemplateVariablesResponse> {
    return this.http.get<TemplateVariablesResponse>(`${this.apiUrl}/variables`);
  }
}
