import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApercuCompte, Delegation } from '../models/delegation.model';

@Injectable({
  providedIn: 'root'
})
export class DelegationService {
  private apiUrl = `${environment.apiUrl}/delegations`;

  constructor(private http: HttpClient) {}

  getMesDelegations(): Observable<Delegation[]> {
    return this.http.get<Delegation[]>(this.apiUrl);
  }

  inviter(email: string): Observable<Delegation> {
    return this.http.post<Delegation>(this.apiUrl, { email });
  }

  revoquer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getDelegationsRecues(): Observable<Delegation[]> {
    return this.http.get<Delegation[]>(`${this.apiUrl}/recues`);
  }

  getApercu(id: string): Observable<ApercuCompte> {
    return this.http.get<ApercuCompte>(`${this.apiUrl}/${id}/apercu`);
  }
}
