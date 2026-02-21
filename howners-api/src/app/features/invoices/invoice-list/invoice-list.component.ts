import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { InvoiceService } from '../../../core/services/invoice.service';
import {
  Invoice,
  InvoiceStatus,
  InvoiceType,
  INVOICE_STATUS_LABELS,
  INVOICE_STATUS_COLORS,
  INVOICE_TYPE_LABELS
} from '../../../core/models/invoice.model';

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
  selectedStatus: InvoiceStatus | 'ALL' = 'ALL';

  InvoiceStatus = InvoiceStatus;
  statusLabels = INVOICE_STATUS_LABELS;
  statusColors = INVOICE_STATUS_COLORS;
  typeLabels = INVOICE_TYPE_LABELS;

  statuses = [
    { value: 'ALL', label: 'Tous les statuts' },
    ...Object.values(InvoiceStatus).map(s => ({ value: s, label: INVOICE_STATUS_LABELS[s] }))
  ];

  constructor(
    private invoiceService: InvoiceService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
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
      error: (err) => {
        console.error('Error loading invoices:', err);
        this.error = 'Erreur lors du chargement des factures';
        this.loading = false;
      }
    });
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

  onStatusChange(): void { this.applyFilters(); }
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
      error: (err) => {
        console.error('Error downloading PDF:', err);
      }
    });
  }
}
