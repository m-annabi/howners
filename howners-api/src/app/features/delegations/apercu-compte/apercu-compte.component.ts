import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DelegationService } from '../../../core/services/delegation.service';
import { ApercuCompte } from '../../../core/models/delegation.model';

@Component({
  selector: 'app-apercu-compte',
  templateUrl: './apercu-compte.component.html',
  styleUrls: ['./apercu-compte.component.scss']
})
export class ApercuCompteComponent implements OnInit {
  apercu: ApercuCompte | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private delegationService: DelegationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.loading = true;
    this.delegationService.getApercu(id).subscribe({
      next: (apercu) => {
        this.apercu = apercu;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Accès refusé à ce compte.';
        this.loading = false;
      }
    });
  }

  getBarHeight(value: number): number {
    if (!this.apercu) return 0;
    const max = Math.max(...this.apercu.finances.monthlyBreakdown.map(m => m.revenue), 1);
    return Math.round((value / max) * 100);
  }
}
