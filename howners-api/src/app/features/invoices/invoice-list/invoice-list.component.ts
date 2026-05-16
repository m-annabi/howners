import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { InvoiceService } from '../../../core/services/invoice.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Invoice,
  InvoiceStatus,
  InvoiceType,
  INVOICE_STATUS_LABELS,
  INVOICE_STATUS_COLORS,
  INVOICE_TYPE_LABELS
} from '../../../core/models/invoice.model';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

@Component({
  selector: 'app-invoice-list',
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.scss']
})
export class InvoiceListComponent implements OnInit {
  invoices: Invoice[] = [];
  filteredInvoices: Invoice[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedStatus: string = 'ALL';

  InvoiceStatus = InvoiceStatus;
  statusLabels = INVOICE_STATUS_LABELS;
  statusColors = INVOICE_STATUS_COLORS;
  typeLabels = INVOICE_TYPE_LABELS;

  constructor(
    private invoiceService: InvoiceService,
    private notifications: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  get filters(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('ALL', this.invoices.length);
    for (const i of this.invoices) {
      counts.set(i.status, (counts.get(i.status) || 0) + 1);
    }
    const list: QuickFilter[] = [
      { key: 'ALL', label: 'Toutes', count: counts.get('ALL') || 0 }
    ];
    for (const s of Object.values(InvoiceStatus)) {
      const c = counts.get(s) || 0;
      if (c > 0) {
        list.push({ key: s, label: this.statusLabels[s], count: c });
      }
    }
    return list;
  }

  loadInvoices(): void {
    this.loading = true;
    this.error = null;

    this.invoiceService.getAll().subscribe({
      next: (invoices) => {
        this.invoices = invoices;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des factures';
        this.loading = false;
      }
    });
  }

  onFilterChange(key: string): void {
    this.selectedStatus = key;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.invoices;

    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(i => i.status === this.selectedStatus);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(i =>
        i.invoiceNumber.toLowerCase().includes(term) ||
        i.propertyName.toLowerCase().includes(term) ||
        i.tenantName.toLowerCase().includes(term)
      );
    }

    this.filteredInvoices = filtered;
  }

  onSearchChange(): void { this.applyFilters(); }

  viewInvoice(invoice: Invoice): void {
    this.router.navigate(['/invoices', invoice.id]);
  }

  downloadPdf(invoice: Invoice, event: Event): void {
    event.stopPropagation();
    this.invoiceService.downloadPdf(invoice.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facture-${invoice.invoiceNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.notifications.error(
          `Impossible de télécharger la facture ${invoice.invoiceNumber}. Réessayez ou contactez le support.`);
      }
    });
  }
}
