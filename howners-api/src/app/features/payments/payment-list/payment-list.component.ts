import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
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
import { ReceiptService } from '../../../core/services/receipt.service';
import { InvoiceService } from '../../../core/services/invoice.service';
import { Receipt } from '../../../core/models/receipt.model';
import { Invoice, INVOICE_TYPE_LABELS } from '../../../core/models/invoice.model';
import { downloadBlob } from '../../../shared/utils/file.utils';

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

  /** Vue locataire unifiée : quittance rattachée à chaque échéance payée, factures diverses en dessous. */
  receiptsByPayment = new Map<string, Receipt>();
  invoices: Invoice[] = [];
  invoiceTypeLabels = INVOICE_TYPE_LABELS;

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
    private authService: AuthService,
    private confirmDialog: ConfirmDialogService,
    private receiptService: ReceiptService,
    private invoiceService: InvoiceService
  ) {}

  /** Bail sur lequel la liste est restreinte (arrivée depuis un détail de location). */
  rentalId: string | null = null;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const f = params['filter'];
      if (f === 'late') this.selectedStatus = 'LATE';
      else if (Object.values(PaymentStatus).includes(f)) this.selectedStatus = f;

      this.rentalId = params['rentalId'] || null;
      this.loadPayments();
      if (this.isTenant) this.loadReceiptsAndInvoices();
    });
  }

  private loadReceiptsAndInvoices(): void {
    this.receiptService.getAll().subscribe({
      next: (receipts) => {
        this.receiptsByPayment = new Map(receipts.map(r => [r.paymentId, r]));
      },
      error: () => {}
    });
    this.invoiceService.getAll().subscribe({
      next: (invoices) => {
        this.invoices = this.rentalId ? invoices.filter(i => i.rentalId === this.rentalId) : invoices;
      },
      error: () => {}
    });
  }

  receiptFor(payment: Payment): Receipt | undefined {
    return this.receiptsByPayment.get(payment.id);
  }

  downloadReceipt(payment: Payment, event: Event): void {
    event.stopPropagation();
    const receipt = this.receiptFor(payment);
    if (!receipt) return;
    this.receiptService.downloadPdf(receipt.id).subscribe({
      next: (blob) => downloadBlob(blob, `quittance-${receipt.receiptNumber}.pdf`),
      error: () => this.notificationService.error('Quittance indisponible')
    });
  }

  downloadInvoice(invoice: Invoice, event: Event): void {
    event.stopPropagation();
    this.invoiceService.downloadPdf(invoice.id).subscribe({
      next: (blob) => downloadBlob(blob, `facture-${invoice.invoiceNumber}.pdf`),
      error: () => this.notificationService.error('Facture indisponible')
    });
  }

  /** Retire le filtre par bail et revient à tous les paiements. */
  clearRentalFilter(): void {
    this.router.navigate(['/payments']);
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

    const source = this.rentalId
      ? this.paymentService.getByRental(this.rentalId)
      : this.paymentService.getAll();

    source.subscribe({
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

  relancer(payment: Payment, event: Event): void {
    event.stopPropagation();
    const action = payment.relanceNiveau === 0 ? 'une relance' : 'la mise en demeure';
    this.confirmDialog.confirm('Confirmation', `Envoyer ${action} à ${payment.payerName} pour ${payment.amount} ${payment.currency} ?`, 'warning').subscribe(ok => {
      if (!ok) return;

      this.paymentService.relancer(payment.id).subscribe({
        next: (updated) => {
          const idx = this.payments.findIndex(p => p.id === updated.id);
          if (idx >= 0) this.payments[idx] = updated;
          this.applyFilters();
          this.notificationService.success(payment.relanceNiveau === 0
            ? 'Relance envoyée au locataire'
            : 'Mise en demeure envoyée et archivée');
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Impossible d\'envoyer la relance');
        }
      });
    });
  }

  markPaid(payment: Payment, event: Event): void {
    event.stopPropagation();
    if (payment.status === PaymentStatus.PAID) return;
    this.confirmDialog.confirm('Confirmation', `Confirmer le paiement de ${payment.amount} ${payment.currency} ?`, 'warning').subscribe(ok => {
      if (!ok) return;

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
    });
  }
}
