import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantScore } from '../models/tenant-score.model';

@Injectable({
  providedIn: 'root'
})
export class TenantScoringService {
  private apiUrl = `${environment.apiUrl}/tenant-scoring`;

  constructor(private http: HttpClient) {}

  getScore(tenantId: string): Observable<TenantScore> {
    return this.http.get<TenantScore>(`${this.apiUrl}/${tenantId}`);
  }

  compareScores(tenantIds: string[]): Observable<TenantScore[]> {
    let params = new HttpParams();
    tenantIds.forEach(id => {
      params = params.append('tenantIds', id);
    });
    return this.http.get<TenantScore[]>(`${this.apiUrl}/compare`, { params });
  }
}
