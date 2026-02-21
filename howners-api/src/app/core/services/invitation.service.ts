import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Invitation, CreateInvitationRequest, InvitationStatus } from '../models/invitation.model';

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private apiUrl = `${environment.apiUrl}/invitations`;

  constructor(private http: HttpClient) {}

  invite(request: CreateInvitationRequest): Observable<Invitation> {
    return this.http.post<Invitation>(this.apiUrl, request);
  }

  getReceivedInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.apiUrl}/received`);
  }

  getSentInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.apiUrl}/sent`);
  }

  updateStatus(id: string, status: InvitationStatus): Observable<Invitation> {
    const params = new HttpParams().set('status', status);
    return this.http.put<Invitation>(`${this.apiUrl}/${id}/status`, {}, { params });
  }
}
