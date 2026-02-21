import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Expense,
  CreateExpenseRequest,
  UpdateExpenseRequest
} from '../models/expense.model';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  private apiUrl = `${environment.apiUrl}/expenses`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Expense[]> {
    return this.http.get<Expense[]>(this.apiUrl);
  }

  getById(id: string): Observable<Expense> {
    return this.http.get<Expense>(`${this.apiUrl}/${id}`);
  }

  getByProperty(propertyId: string): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.apiUrl}/property/${propertyId}`);
  }

  create(request: CreateExpenseRequest, file?: File): Observable<Expense> {
    if (file) {
      const formData = new FormData();
      formData.append('expense', new Blob([JSON.stringify(request)], { type: 'application/json' }));
      formData.append('file', file);
      return this.http.post<Expense>(this.apiUrl, formData);
    }
    return this.http.post<Expense>(this.apiUrl, request);
  }

  update(id: string, request: UpdateExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
