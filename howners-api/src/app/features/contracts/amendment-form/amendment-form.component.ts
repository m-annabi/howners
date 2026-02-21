import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContractAmendmentService } from '../../../core/services/contract-amendment.service';
import { ContractService } from '../../../core/services/contract.service';
import { CreateAmendmentRequest } from '../../../core/models/contract-amendment.model';
import { Contract } from '../../../core/models/contract.model';

@Component({
  selector: 'app-amendment-form',
  templateUrl: './amendment-form.component.html'
})
export class AmendmentFormComponent implements OnInit {
  contractId = '';
  contract: Contract | null = null;
  loading = false;
  submitting = false;

  reason = '';
  changes = '';
  previousRent: number | null = null;
  newRent: number | null = null;
  effectiveDate = '';

  constructor(
    private amendmentService: ContractAmendmentService,
    private contractService: ContractService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.contractId = this.route.snapshot.paramMap.get('id') || '';
    this.loadContract();
  }

  loadContract(): void {
    this.loading = true;
    this.contractService.getContract(this.contractId).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (!this.reason || !this.effectiveDate) return;

    this.submitting = true;
    const request: CreateAmendmentRequest = {
      reason: this.reason,
      changes: this.changes || undefined,
      previousRent: this.previousRent || undefined,
      newRent: this.newRent || undefined,
      effectiveDate: this.effectiveDate
    };

    this.amendmentService.create(this.contractId, request).subscribe({
      next: () => {
        this.router.navigate(['/contracts', this.contractId, 'amendments']);
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/contracts', this.contractId, 'amendments']);
  }
}
