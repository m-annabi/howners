import { Component, OnInit } from '@angular/core';
import { SignatureTrackingService } from '../../../core/services/signature-tracking.service';
import {
  SignatureTrackingDashboard,
  SignatureRequest,
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
  statusLabels = SIGNATURE_STATUS_LABELS;
  statusColors = SIGNATURE_STATUS_COLORS;

  constructor(private trackingService: SignatureTrackingService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.trackingService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getSignedPercentage(): number {
    if (!this.dashboard || this.dashboard.totalRequests === 0) return 0;
    return Math.round((this.dashboard.signedRequests / this.dashboard.totalRequests) * 100);
  }
}
