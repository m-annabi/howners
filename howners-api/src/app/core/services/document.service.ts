import { formatFileSize } from '../../shared/utils/file.utils';
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
    return formatFileSize(bytes);
  }

  /**
   * Classe Bootstrap Icons correspondant à un type MIME.
   * mimeType peut être absent sur d'anciens documents : on retombe sur l'icône générique.
   */
  getFileIcon(mimeType?: string | null): string {
    if (!mimeType) return 'bi-file-earmark';
    if (mimeType.startsWith('image/')) return 'bi-file-earmark-image';
    if (mimeType === 'application/pdf') return 'bi-file-earmark-pdf';
    if (mimeType.includes('word')) return 'bi-file-earmark-word';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return 'bi-file-earmark-spreadsheet';
    return 'bi-file-earmark';
  }
}
