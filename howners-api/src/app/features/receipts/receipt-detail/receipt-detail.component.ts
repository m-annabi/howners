import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ReceiptService } from '../../../core/services/receipt.service';
import { Receipt } from '../../../core/models/receipt.model';

@Component({
  selector: 'app-receipt-detail',
  templateUrl: './receipt-detail.component.html',
  styles: []
})
export class ReceiptDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  receipt: Receipt | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private receiptService: ReceiptService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadReceipt(id);
    }
  }

  loadReceipt(id: string): void {
    this.loading = true;
    this.receiptService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (receipt) => {
        this.receipt = receipt;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading receipt:', err);
        this.error = 'Erreur lors du chargement de la quittance';
        this.loading = false;
      }
    });
  }

  downloadPdf(): void {
    if (!this.receipt) return;
    this.receiptService.downloadPdf(this.receipt.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `quittance-${this.receipt!.receiptNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading PDF:', err);
      }
    });
  }

  viewDocument(): void {
    if (this.receipt?.documentUrl) {
      window.open(this.receipt.documentUrl, '_blank');
    }
  }

  goBack(): void {
    this.router.navigate(['/receipts']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
