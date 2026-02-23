import { Component, OnInit } from '@angular/core';
import { SignatureTrackingService } from '../../../core/services/signature-tracking.service';
import {
  SignatureTrackingDashboard,
  SIGNATURE_STATUS_LABELS,
  SIGNATURE_STATUS_COLORS
} from '../../../core/models/signature-tracking.model';

@Component({
  selector: 'app-signature-dashboard',
  templateUrl: './signature-dashboard.component.html'
})
export class SignatureDashboardComponent implements OnInit {
  dashboard: SignatureTrackingDashboard | null = null;
  loading = true;
  error: string | null = null;
  statusLabels = SIGNATURE_STATUS_LABELS;
  statusColors = SIGNATURE_STATUS_COLORS;

  constructor(private trackingService: SignatureTrackingService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;
    this.trackingService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Impossible de charger le tableau de bord. Veuillez réessayer.';
      }
    });
  }

  getSignedPercentage(): number {
    if (!this.dashboard || this.dashboard.totalRequests === 0) return 0;
    return Math.round((this.dashboard.signedRequests / this.dashboard.totalRequests) * 100);
  }
}
