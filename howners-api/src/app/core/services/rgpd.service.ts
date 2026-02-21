import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConsentRequest, ConsentResponse, UserDataExport } from '../models/rgpd.model';

@Injectable({
  providedIn: 'root'
})
export class RgpdService {
  private apiUrl = `${environment.apiUrl}/rgpd`;

  constructor(private http: HttpClient) {}

  exportData(): Observable<UserDataExport> {
    return this.http.get<UserDataExport>(`${this.apiUrl}/export`);
  }

  exportDataAsPdf(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/pdf`, { responseType: 'blob' });
  }

  requestErasure(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/erasure`, {});
  }

  getConsents(): Observable<ConsentResponse[]> {
    return this.http.get<ConsentResponse[]>(`${this.apiUrl}/consent`);
  }

  recordConsent(request: ConsentRequest): Observable<ConsentResponse> {
    return this.http.post<ConsentResponse>(`${this.apiUrl}/consent`, request);
  }

  deleteConsent(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/consent/${id}`);
  }
}
