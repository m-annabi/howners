import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { SignatureRequestResponse } from '../models/esignature.model';

/**
 * Service pour la gestion des signatures électroniques (endpoints authentifiés)
 */
@Injectable({
  providedIn: 'root'
})
export class EsignatureService {
  private readonly apiUrl = `${environment.apiUrl}/contracts`;

  constructor(private http: HttpClient) {}

  /**
   * Envoie un contrat pour signature électronique
   */
  sendForSignature(contractId: string): Observable<SignatureRequestResponse> {
    return this.http.post<SignatureRequestResponse>(
      `${this.apiUrl}/${contractId}/esignature/send`,
      {}
    ).pipe(
      catchError(error => {
        console.error('Error sending contract for signature:', error);
        const userMessage = this.extractErrorMessage(error, 'send');
        return throwError(() => ({ ...error, userMessage }));
      })
    );
  }

  /**
   * Récupère le statut d'une demande de signature
   */
  getSignatureStatus(contractId: string): Observable<SignatureRequestResponse> {
    return this.http.get<SignatureRequestResponse>(
      `${this.apiUrl}/${contractId}/esignature/status`
    ).pipe(
      catchError(error => {
        console.error('Error getting signature status:', error);
        const userMessage = this.extractErrorMessage(error, 'status');
        return throwError(() => ({ ...error, userMessage }));
      })
    );
  }

  /**
   * Renvoie une demande de signature
   */
  resendSignatureRequest(contractId: string, signatureRequestId: string): Observable<void> {
    const params = new HttpParams().set('signatureRequestId', signatureRequestId);
    return this.http.post<void>(
      `${this.apiUrl}/${contractId}/esignature/resend`,
      {},
      { params }
    ).pipe(
      catchError(error => {
        console.error('Error resending signature request:', error);
        const userMessage = this.extractErrorMessage(error, 'resend');
        return throwError(() => ({ ...error, userMessage }));
      })
    );
  }

  /**
   * Annule une demande de signature
   */
  cancelSignatureRequest(contractId: string, signatureRequestId: string): Observable<void> {
    const params = new HttpParams().set('signatureRequestId', signatureRequestId);
    return this.http.delete<void>(
      `${this.apiUrl}/${contractId}/esignature/cancel`,
      { params }
    ).pipe(
      catchError(error => {
        console.error('Error cancelling signature request:', error);
        const userMessage = this.extractErrorMessage(error, 'cancel');
        return throwError(() => ({ ...error, userMessage }));
      })
    );
  }

  /**
   * Extract user-friendly error message from HTTP error
   */
  private extractErrorMessage(error: any, operation: string): string {
    // If backend provided a message, use it
    if (error.error?.message) {
      return error.error.message;
    }

    // Otherwise, provide context-specific messages based on status code
    switch (error.status) {
      case 404:
        return operation === 'status'
          ? 'Demande de signature non trouvée'
          : 'Contrat non trouvé';
      case 400:
        return 'Le contrat ne peut pas être envoyé dans son état actuel';
      case 401:
        return 'Non autorisé à effectuer cette action';
      case 410:
        return 'Le lien de signature a expiré';
      case 500:
        return 'Erreur serveur lors du traitement de la demande';
      case 502:
        return 'Erreur de communication avec le service de signature';
      default:
        return `Une erreur inattendue est survenue lors de l'opération: ${operation}`;
    }
  }
}
