import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Application, CreateApplicationRequest, CreateRentalFromApplicationRequest, ReviewApplicationRequest } from '../models/application.model';
import { Rental } from '../models/rental.model';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private apiUrl = `${environment.apiUrl}/applications`;

  constructor(private http: HttpClient) {}

  submit(request: CreateApplicationRequest): Observable<Application> {
    return this.http.post<Application>(this.apiUrl, request);
  }

  getMyApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/my`);
  }

  getReceivedApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/received`);
  }

  getByListing(listingId: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/listing/${listingId}`);
  }

  getById(id: string): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${id}`);
  }

  review(id: string, request: ReviewApplicationRequest): Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${id}/review`, request);
  }

  withdraw(id: string): Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${id}/withdraw`, {});
  }

  createRentalFromApplication(applicationId: string, request: CreateRentalFromApplicationRequest): Observable<Rental> {
    return this.http.post<Rental>(`${this.apiUrl}/${applicationId}/create-rental`, request);
  }
}
