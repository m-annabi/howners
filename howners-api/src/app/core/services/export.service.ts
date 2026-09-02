import { downloadBlob } from '../../shared/utils/file.utils';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ExportService {
  private readonly API_URL = `${environment.apiUrl}/export`;

  constructor(private http: HttpClient) {}

  downloadFinancialCsv(year: number): void {
    this.http.get(`${this.API_URL}/financial?year=${year}&format=csv`, {
      responseType: 'blob'
    }).subscribe(blob => {
      downloadBlob(blob, `export-comptable-${year}.csv`);
    });
  }
}
