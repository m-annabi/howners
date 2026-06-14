import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RefereeItem {
  name: string;
  status: 'PENDING' | 'CONVERTED';
  createdAt: string;
  rewardedAt: string | null;
}

export interface ReferralSummary {
  code: string;
  shareUrl: string;
  pendingCount: number;
  convertedCount: number;
  referees: RefereeItem[];
}

export interface ReferralCodeResponse {
  code: string;
  link: string;
}

export interface ReferralStatsResponse {
  total: number;
  successful: number;
  pending: number;
}

@Injectable({ providedIn: 'root' })
export class ReferralService {
  private apiUrl = `${environment.apiUrl}/referrals`;

  constructor(private http: HttpClient) {}

  getMySummary(): Observable<ReferralSummary> {
    return this.http.get<ReferralSummary>(`${this.apiUrl}/me`);
  }

  getMyCode(): Observable<ReferralCodeResponse> {
    return this.http.get<ReferralCodeResponse>(`${this.apiUrl}/my-code`);
  }

  getStats(): Observable<ReferralStatsResponse> {
    return this.http.get<ReferralStatsResponse>(`${this.apiUrl}/stats`);
  }

  applyCode(code: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/apply`, { code });
  }
}
