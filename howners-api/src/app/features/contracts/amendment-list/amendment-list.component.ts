import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContractAmendmentService } from '../../../core/services/contract-amendment.service';
import {
  ContractAmendment,
  AMENDMENT_STATUS_LABELS,
  AMENDMENT_STATUS_COLORS
} from '../../../core/models/contract-amendment.model';

@Component({
  selector: 'app-amendment-list',
  templateUrl: './amendment-list.component.html'
})
export class AmendmentListComponent implements OnInit {
  amendments: ContractAmendment[] = [];
  contractId = '';
  loading = true;
  statusLabels = AMENDMENT_STATUS_LABELS;
  statusColors = AMENDMENT_STATUS_COLORS;

  constructor(
    private amendmentService: ContractAmendmentService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.contractId = this.route.snapshot.paramMap.get('id') || '';
    this.loadAmendments();
  }

  loadAmendments(): void {
    this.loading = true;
    this.amendmentService.getByContract(this.contractId).subscribe({
      next: (data) => {
        this.amendments = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  createAmendment(): void {
    this.router.navigate(['/contracts', this.contractId, 'amendments', 'new']);
  }

  signAmendment(amendment: ContractAmendment): void {
    if (confirm('Êtes-vous sûr de vouloir signer cet avenant ?')) {
      this.amendmentService.sign(this.contractId, amendment.id).subscribe({
        next: () => this.loadAmendments()
      });
    }
  }

  downloadPdf(amendment: ContractAmendment): void {
    this.amendmentService.downloadPdf(this.contractId, amendment.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `avenant-${amendment.amendmentNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/contracts', this.contractId]);
  }
}
