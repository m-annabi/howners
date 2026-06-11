import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Property, CreatePropertyRequest, UpdatePropertyRequest } from '../../core/models/property.model';
import { Page } from '../../core/models/page.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyService {
  private readonly API_URL = `${environment.apiUrl}/properties`;

  constructor(private http: HttpClient) {}

  getProperties(page?: number, size?: number): Observable<Page<Property>> {
    let params = new HttpParams();
    if (page != null) params = params.set('page', page.toString());
    if (size != null) params = params.set('size', size.toString());
    return this.http.get<Page<Property>>(this.API_URL, { params });
  }

  getProperty(id: string): Observable<Property> {
    return this.http.get<Property>(`${this.API_URL}/${id}`);
  }

  createProperty(request: CreatePropertyRequest): Observable<Property> {
    return this.http.post<Property>(this.API_URL, request);
  }

  updateProperty(id: string, request: UpdatePropertyRequest): Observable<Property> {
    return this.http.put<Property>(`${this.API_URL}/${id}`, request);
  }

  deleteProperty(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
