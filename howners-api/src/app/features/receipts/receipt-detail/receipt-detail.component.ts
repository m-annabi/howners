import { NavigationService } from '../../../core/services/navigation.service';
import { downloadBlob } from '../../../shared/utils/file.utils';
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
    private receiptService: ReceiptService,
    private nav: NavigationService
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
      error: () => {
        this.error = 'Erreur lors du chargement de la quittance';
        this.loading = false;
      }
    });
  }

  downloadPdf(): void {
    if (!this.receipt) return;
    this.receiptService.downloadPdf(this.receipt.id).subscribe({
      next: (blob) => downloadBlob(blob, `quittance-${this.receipt!.receiptNumber}.pdf`),
      error: () => {
      }
    });
  }

  viewDocument(): void {
    if (this.receipt?.documentUrl) {
      window.open(this.receipt.documentUrl, '_blank');
    }
  }

  goBack(): void {
    this.nav.back(['/receipts']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
