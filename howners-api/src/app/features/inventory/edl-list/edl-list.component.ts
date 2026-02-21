import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import {
  EtatDesLieux,
  EDL_TYPE_LABELS,
  EDL_TYPE_COLORS
} from '../../../core/models/etat-des-lieux.model';

@Component({
  selector: 'app-edl-list',
  templateUrl: './edl-list.component.html'
})
export class EdlListComponent implements OnInit {
  edls: EtatDesLieux[] = [];
  filteredEdls: EtatDesLieux[] = [];
  loading = true;
  typeLabels = EDL_TYPE_LABELS;
  typeColors = EDL_TYPE_COLORS;
  filterType = '';

  constructor(
    private edlService: EtatDesLieuxService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEdls();
  }

  loadEdls(): void {
    this.loading = true;
    this.edlService.getMyEdls().subscribe({
      next: (data) => {
        this.edls = data;
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    if (this.filterType) {
      this.filteredEdls = this.edls.filter(e => e.type === this.filterType);
    } else {
      this.filteredEdls = [...this.edls];
    }
  }

  viewDetail(edl: EtatDesLieux): void {
    this.router.navigate(['/inventory', edl.rentalId, edl.id]);
  }

  getSignatureStatus(edl: EtatDesLieux): string {
    if (edl.ownerSigned && edl.tenantSigned) return 'Complet';
    if (edl.ownerSigned) return 'Bailleur uniquement';
    if (edl.tenantSigned) return 'Locataire uniquement';
    return 'Non signé';
  }

  getSignatureBadge(edl: EtatDesLieux): string {
    if (edl.ownerSigned && edl.tenantSigned) return 'success';
    if (edl.ownerSigned || edl.tenantSigned) return 'warning';
    return 'secondary';
  }
}
