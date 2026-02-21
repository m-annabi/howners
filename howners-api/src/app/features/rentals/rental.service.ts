import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rental, CreateRentalRequest, UpdateRentalRequest } from '../../core/models/rental.model';
import { User } from '../../core/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class RentalService {
  private readonly API_URL = `${environment.apiUrl}/rentals`;

  constructor(private http: HttpClient) {}

  getRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(this.API_URL);
  }

  getRental(id: string): Observable<Rental> {
    return this.http.get<Rental>(`${this.API_URL}/${id}`);
  }

  createRental(request: CreateRentalRequest): Observable<Rental> {
    return this.http.post<Rental>(this.API_URL, request);
  }

  updateRental(id: string, request: UpdateRentalRequest): Observable<Rental> {
    return this.http.put<Rental>(`${this.API_URL}/${id}`, request);
  }

  deleteRental(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getMyTenants(): Observable<User[]> {
    return this.http.get<User[]>(`${this.API_URL}/my-tenants`);
  }
}
