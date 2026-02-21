import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Document, UploadDocumentRequest } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  /**
   * Upload un document
   */
  uploadDocument(request: UploadDocumentRequest): Observable<Document> {
    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('documentType', request.documentType);

    if (request.propertyId) {
      formData.append('propertyId', request.propertyId);
    }

    if (request.rentalId) {
      formData.append('rentalId', request.rentalId);
    }

    if (request.applicationId) {
      formData.append('applicationId', request.applicationId);
    }

    if (request.description) {
      formData.append('description', request.description);
    }

    return this.http.post<Document>(`${this.apiUrl}/upload`, formData);
  }

  /**
   * Récupérer tous les documents de l'utilisateur
   */
  getMyDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl);
  }

  /**
   * Récupérer un document par ID
   */
  getDocument(id: string): Observable<Document> {
    return this.http.get<Document>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les documents d'une propriété
   */
  getPropertyDocuments(propertyId: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/property/${propertyId}`);
  }

  /**
   * Récupérer les documents d'une location
   */
  getRentalDocuments(rentalId: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/rental/${rentalId}`);
  }

  /**
   * Récupérer les documents d'une candidature
   */
  getApplicationDocuments(applicationId: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/application/${applicationId}`);
  }

  /**
   * Télécharger un document
   */
  downloadDocument(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      responseType: 'blob'
    });
  }

  /**
   * Supprimer un document
   */
  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Définir la période de rétention d'un document
   */
  setRetention(id: string, retentionEndDate: string): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}/retention`, { retentionEndDate });
  }

  /**
   * Archiver un document
   */
  archiveDocument(id: string): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}/archive`, {});
  }

  /**
   * Mettre/retirer un blocage légal
   */
  setLegalHold(id: string, hold: boolean): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}/legal-hold`, { hold });
  }

  /**
   * Formater la taille d'un fichier
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Obtenir l'icône pour un type MIME
   */
  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType === 'application/pdf') return '📄';
    if (mimeType.includes('word')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
    return '📎';
  }
}
