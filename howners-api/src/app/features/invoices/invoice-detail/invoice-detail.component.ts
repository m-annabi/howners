import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { InvoiceService } from '../../../core/services/invoice.service';
import {
  Invoice,
  InvoiceStatus,
  INVOICE_STATUS_LABELS,
  INVOICE_STATUS_COLORS,
  INVOICE_TYPE_LABELS
} from '../../../core/models/invoice.model';

@Component({
  selector: 'app-invoice-detail',
  templateUrl: './invoice-detail.component.html',
  styles: []
})
export class InvoiceDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  invoice: Invoice | null = null;
  loading = false;
  error: string | null = null;

  statusLabels = INVOICE_STATUS_LABELS;
  statusColors = INVOICE_STATUS_COLORS;
  typeLabels = INVOICE_TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(id);
    }
  }

  loadInvoice(id: string): void {
    this.loading = true;
    this.invoiceService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (invoice) => {
        this.invoice = invoice;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading invoice:', err);
        this.error = 'Erreur lors du chargement de la facture';
        this.loading = false;
      }
    });
  }

  downloadPdf(): void {
    if (!this.invoice) return;
    this.invoiceService.downloadPdf(this.invoice.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facture-${this.invoice!.invoiceNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading PDF:', err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/invoices']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
