import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WidgetConfig, WidgetPage } from '../models/widget-config.model';

@Injectable({ providedIn: 'root' })
export class WidgetPreferenceService {
  private readonly apiUrl = `${environment.apiUrl}/preferences/widgets`;

  constructor(private http: HttpClient) {}

  getPreferences(page: WidgetPage): Observable<WidgetConfig[]> {
    return this.http.get<WidgetConfig[]>(`${this.apiUrl}/${page}`);
  }

  savePreferences(page: WidgetPage, widgets: WidgetConfig[]): Observable<WidgetConfig[]> {
    return this.http.put<WidgetConfig[]>(`${this.apiUrl}/${page}`, { widgets });
  }
}
