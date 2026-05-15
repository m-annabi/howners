import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

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
  selectedStatus: string = 'ALL';

  PaymentStatus = PaymentStatus;
  statusLabels = PAYMENT_STATUS_LABELS;
  statusColors = PAYMENT_STATUS_COLORS;
  typeLabels = PAYMENT_TYPE_LABELS;

  get isTenant(): boolean {
    return this.authService.hasRole('TENANT');
  }

  constructor(
    private paymentService: PaymentService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const f = params['filter'];
      if (f === 'late') this.selectedStatus = 'LATE';
      else if (Object.values(PaymentStatus).includes(f)) this.selectedStatus = f;
    });
    this.loadPayments();
  }

  get filters(): QuickFilter[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const counts = new Map<string, number>();
    counts.set('ALL', this.payments.length);
    let lateCount = 0;
    for (const p of this.payments) {
      counts.set(p.status, (counts.get(p.status) || 0) + 1);
      const isPastDue = p.dueDate && new Date(p.dueDate) < today;
      if (p.status === PaymentStatus.LATE || (p.status === PaymentStatus.PENDING && isPastDue)) {
        lateCount++;
      }
    }

    const list: QuickFilter[] = [
      { key: 'ALL', label: 'Tous', count: counts.get('ALL') || 0 },
      { key: 'LATE', label: 'En retard', count: lateCount, tone: 'danger' },
      { key: PaymentStatus.PENDING, label: 'À venir', count: counts.get(PaymentStatus.PENDING) || 0, tone: 'warning' },
      { key: PaymentStatus.PAID, label: 'Payés', count: counts.get(PaymentStatus.PAID) || 0, tone: 'success' },
      { key: PaymentStatus.FAILED, label: 'Échoués', count: counts.get(PaymentStatus.FAILED) || 0 },
      { key: PaymentStatus.REFUNDED, label: 'Remboursés', count: counts.get(PaymentStatus.REFUNDED) || 0 }
    ];
    return list.filter(f => f.key === 'ALL' || (f.count || 0) > 0);
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
      error: () => {
        this.error = 'Erreur lors du chargement des paiements';
        this.loading = false;
      }
    });
  }

  onFilterChange(key: string): void {
    this.selectedStatus = key;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.payments;
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (this.selectedStatus === 'LATE') {
      filtered = filtered.filter(p =>
        p.status === PaymentStatus.LATE ||
        (p.status === PaymentStatus.PENDING && p.dueDate && new Date(p.dueDate) < today)
      );
    } else if (this.selectedStatus !== 'ALL') {
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

  onSearchChange(): void { this.applyFilters(); }

  isOverdue(payment: Payment): boolean {
    if (payment.status === PaymentStatus.PAID || payment.status === PaymentStatus.REFUNDED) return false;
    if (!payment.dueDate) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(payment.dueDate) < today;
  }

  getStatusColor(status: PaymentStatus): string { return this.statusColors[status]; }
  getStatusLabel(status: PaymentStatus): string { return this.statusLabels[status]; }
  getTypeLabel(type: PaymentType): string { return this.typeLabels[type]; }

  viewPayment(payment: Payment): void {
    this.router.navigate(['/payments', payment.id]);
  }

  createPayment(): void {
    this.router.navigate(['/payments/new']);
  }

  markPaid(payment: Payment, event: Event): void {
    event.stopPropagation();
    if (payment.status === PaymentStatus.PAID) return;
    if (!confirm(`Confirmer le paiement de ${payment.amount} ${payment.currency} ?`)) return;

    this.paymentService.confirmPayment(payment.id).subscribe({
      next: (updated) => {
        const idx = this.payments.findIndex(p => p.id === updated.id);
        if (idx >= 0) this.payments[idx] = updated;
        this.applyFilters();
        this.notificationService.success('Paiement marqué comme payé');
      },
      error: () => {
        this.notificationService.error('Impossible de confirmer le paiement');
      }
    });
  }
}
