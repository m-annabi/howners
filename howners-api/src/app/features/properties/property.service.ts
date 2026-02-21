import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Property, CreatePropertyRequest, UpdatePropertyRequest } from '../../core/models/property.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyService {
  private readonly API_URL = `${environment.apiUrl}/properties`;

  constructor(private http: HttpClient) {}

  getProperties(): Observable<Property[]> {
    return this.http.get<Property[]>(this.API_URL);
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
