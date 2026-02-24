import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  Payment,
  PaymentStatus,
  PaymentType,
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_COLORS,
  PAYMENT_TYPE_LABELS
} from '../../../core/models/payment.model';

@Component({
  selector: 'app-payment-list',
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.scss']
})
export class PaymentListComponent implements OnInit {
  payments: Payment[] = [];
  filteredPayments: Payment[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedStatus: PaymentStatus | 'ALL' = 'ALL';

  PaymentStatus = PaymentStatus;
  statusLabels = PAYMENT_STATUS_LABELS;
  statusColors = PAYMENT_STATUS_COLORS;
  typeLabels = PAYMENT_TYPE_LABELS;

  statuses = [
    { value: 'ALL', label: 'Tous les statuts' },
    ...Object.values(PaymentStatus).map(s => ({ value: s, label: PAYMENT_STATUS_LABELS[s] }))
  ];

  get isTenant(): boolean {
    return this.authService.hasRole('TENANT');
  }

  constructor(
    private paymentService: PaymentService,
    private router: Router,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.loading = true;
    this.error = null;

    this.paymentService.getAll().subscribe({
      next: (payments) => {
        this.payments = payments;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading payments:', err);
        this.error = 'Erreur lors du chargement des paiements';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.payments;

    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(p => p.status === this.selectedStatus);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p =>
        p.propertyName.toLowerCase().includes(term) ||
        p.payerName.toLowerCase().includes(term)
      );
    }

    this.filteredPayments = filtered;
  }

  onStatusChange(): void { this.applyFilters(); }
  onSearchChange(): void { this.applyFilters(); }

  getStatusColor(status: PaymentStatus): string {
    return this.statusColors[status];
  }

  getStatusLabel(status: PaymentStatus): string {
    return this.statusLabels[status];
  }

  getTypeLabel(type: PaymentType): string {
    return this.typeLabels[type];
  }

  viewPayment(payment: Payment): void {
    this.router.navigate(['/payments', payment.id]);
  }

  createPayment(): void {
    this.router.navigate(['/payments/new']);
  }
}
