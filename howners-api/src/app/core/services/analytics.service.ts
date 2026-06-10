import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MonthlyRevenue {
  month: string;
  amount: number;
}

export interface AnalyticsSummary {
  totalRevenue: number;
  occupancyRate: number;
  totalProperties: number;
  activeRentals: number;
  monthlyRevenue: MonthlyRevenue[];
  vacantProperties: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly API_URL = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) {}

  getSummary(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${this.API_URL}/summary`);
  }
}
