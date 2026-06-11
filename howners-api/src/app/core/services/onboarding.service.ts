import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OnboardingStep {
  key: string;
  label: string;
  done: boolean;
  link: string;
}

export interface OnboardingStatus {
  completed: boolean;
  steps: OnboardingStep[];
  completionPercent: number;
}

@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  private readonly API_URL = `${environment.apiUrl}/onboarding`;

  constructor(private http: HttpClient) {}

  getStatus(): Observable<OnboardingStatus> {
    return this.http.get<OnboardingStatus>(`${this.API_URL}/status`);
  }
}
