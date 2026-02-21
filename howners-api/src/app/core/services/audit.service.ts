import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditAction, AuditLog, AuditLogPage } from '../models/audit.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private apiUrl = `${environment.apiUrl}/audit`;

  constructor(private http: HttpClient) {}

  getAll(page: number = 0, size: number = 20, entityType?: string, action?: AuditAction, userId?: string): Observable<AuditLogPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (entityType) params = params.set('entityType', entityType);
    if (action) params = params.set('action', action);
    if (userId) params = params.set('userId', userId);

    return this.http.get<AuditLogPage>(this.apiUrl, { params });
  }

  getByEntity(entityType: string, entityId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/entity/${entityType}/${entityId}`);
  }

  getByUser(userId: string, page: number = 0, size: number = 20): Observable<AuditLogPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AuditLogPage>(`${this.apiUrl}/user/${userId}`, { params });
  }
}
