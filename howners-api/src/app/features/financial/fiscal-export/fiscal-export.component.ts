import { Component, OnInit } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Declaration2044,
  ExportFiscalService,
  LIGNES_2044_LABELS
} from '../../../core/services/export-fiscal.service';

@Component({
  selector: 'app-fiscal-export',
  templateUrl: './fiscal-export.component.html',
  styleUrls: ['./fiscal-export.component.scss']
})
export class FiscalExportComponent implements OnInit {
  declaration: Declaration2044 | null = null;
  loading = false;
  error: string | null = null;
  annee = new Date().getFullYear() - 1;

  lignesLabels = LIGNES_2044_LABELS;

  constructor(
    private exportFiscalService: ExportFiscalService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.exportFiscalService.getApercu(this.annee).subscribe({
      next: (declaration) => {
        this.declaration = declaration;
        this.loading = false;
      },
      error: (err) => {
        this.declaration = null;
        this.error = err.error?.message || 'Erreur lors du calcul de la déclaration.';
        this.loading = false;
      }
    });
  }

  download(format: 'pdf' | 'csv'): void {
    this.exportFiscalService.download(this.annee, format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `declaration-2044-${this.annee}.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notifications.error('Export indisponible.')
    });
  }

  get anneesDisponibles(): number[] {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2, current - 3];
  }

  lignesOf(charges: { [ligne: string]: number }): string[] {
    return Object.keys(charges).sort();
  }
}
