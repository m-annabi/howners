import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReceiptService } from '../../../core/services/receipt.service';
import { Receipt } from '../../../core/models/receipt.model';

@Component({
  selector: 'app-receipt-list',
  templateUrl: './receipt-list.component.html',
  styleUrls: ['./receipt-list.component.scss']
})
export class ReceiptListComponent implements OnInit {
  receipts: Receipt[] = [];
  filteredReceipts: Receipt[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';

  constructor(
    private receiptService: ReceiptService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReceipts();
  }

  loadReceipts(): void {
    this.loading = true;
    this.error = null;

    this.receiptService.getAll().subscribe({
      next: (receipts) => {
        this.receipts = receipts;
        this.filteredReceipts = receipts;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading receipts:', err);
        this.error = 'Erreur lors du chargement des quittances';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    if (!this.searchTerm) {
      this.filteredReceipts = this.receipts;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredReceipts = this.receipts.filter(r =>
      r.receiptNumber.toLowerCase().includes(term) ||
      r.propertyName.toLowerCase().includes(term) ||
      r.tenantName.toLowerCase().includes(term)
    );
  }

  onSearchChange(): void { this.applyFilters(); }

  viewReceipt(receipt: Receipt): void {
    this.router.navigate(['/receipts', receipt.id]);
  }

  downloadPdf(receipt: Receipt, event: Event): void {
    event.stopPropagation();
    this.receiptService.downloadPdf(receipt.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `quittance-${receipt.receiptNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading PDF:', err);
      }
    });
  }
}
