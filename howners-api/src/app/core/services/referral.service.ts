import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RefereeItem {
  name: string;
  status: 'PENDING' | 'CONVERTED';
  createdAt: string;
}

export interface ReferralSummary {
  code: string;
  shareUrl: string;
  pendingCount: number;
  convertedCount: number;
  referees: RefereeItem[];
}

@Injectable({ providedIn: 'root' })
export class ReferralService {
  private apiUrl = `${environment.apiUrl}/referrals`;

  constructor(private http: HttpClient) {}

  getMySummary(): Observable<ReferralSummary> {
    return this.http.get<ReferralSummary>(`${this.apiUrl}/me`);
  }
}
