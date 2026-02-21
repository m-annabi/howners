import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FinancialDashboard } from '../models/financial-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class FinancialDashboardService {
  private apiUrl = `${environment.apiUrl}/financial`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<FinancialDashboard> {
    return this.http.get<FinancialDashboard>(`${this.apiUrl}/dashboard`);
  }
}
