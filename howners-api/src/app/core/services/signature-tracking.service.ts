import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SignatureTrackingDashboard } from '../models/signature-tracking.model';

@Injectable({
  providedIn: 'root'
})
export class SignatureTrackingService {
  private apiUrl = `${environment.apiUrl}/signature-tracking`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<SignatureTrackingDashboard> {
    return this.http.get<SignatureTrackingDashboard>(`${this.apiUrl}/dashboard`);
  }
}
