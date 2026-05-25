import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import {
  EtatDesLieux,
  EtatDesLieuxType,
  EDL_TYPE_LABELS
} from '../../../core/models/etat-des-lieux.model';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

@Component({
  selector: 'app-edl-list',
  templateUrl: './edl-list.component.html',
  styleUrls: ['./edl-list.component.scss']
})
export class EdlListComponent implements OnInit {
  edls: EtatDesLieux[] = [];
  filteredEdls: EtatDesLieux[] = [];
  filters: QuickFilter[] = [];
  loading = true;
  error: string | null = null;
  searchTerm = '';
  activeFilter = '';
  sortCol = '';
  sortDir: 'asc' | 'desc' = 'desc';
  typeLabels = EDL_TYPE_LABELS;

  constructor(
    private edlService: EtatDesLieuxService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEdls();
  }

  private buildFilters(): void {
    const counts = new Map<string, number>();
    counts.set('', this.edls.length);
    for (const e of this.edls) {
      counts.set(e.type, (counts.get(e.type) || 0) + 1);
    }
    this.filters = [
      { key: '', label: 'Tous', count: counts.get('') || 0 },
      { key: EtatDesLieuxType.ENTREE, label: 'Entrée', count: counts.get(EtatDesLieuxType.ENTREE) || 0, tone: 'success' as const },
      { key: EtatDesLieuxType.SORTIE, label: 'Sortie', count: counts.get(EtatDesLieuxType.SORTIE) || 0, tone: 'warning' as const }
    ];
  }

  sortIcon(col: string): string {
    if (this.sortCol !== col) return 'bi-arrow-down-up';
    return this.sortDir === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down';
  }

  sortOn(col: string): void {
    this.sortDir = this.sortCol === col && this.sortDir === 'desc' ? 'asc' : 'desc';
    this.sortCol = col;
    this.applyFilters();
  }

  onFilterChange(key: string): void {
    this.activeFilter = key;
    this.applyFilters();
  }

  loadEdls(): void {
    this.loading = true;
    this.error = null;
    this.edlService.getMyEdls().subscribe({
      next: (data) => {
        this.edls = data;
        this.buildFilters();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des états des lieux';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.edls;

    if (this.activeFilter) {
      filtered = filtered.filter(e => e.type === this.activeFilter);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.trim().toLowerCase();
      filtered = filtered.filter(e =>
        e.propertyName.toLowerCase().includes(term) ||
        (e.tenantName && e.tenantName.toLowerCase().includes(term))
      );
    }

    if (this.sortCol) {
      filtered = filtered.slice().sort((a, b) => {
        let diff = 0;
        if (this.sortCol === 'property') diff = a.propertyName.localeCompare(b.propertyName);
        else if (this.sortCol === 'tenant') diff = (a.tenantName || '').localeCompare(b.tenantName || '');
        else if (this.sortCol === 'type') diff = a.type.localeCompare(b.type);
        else if (this.sortCol === 'date') diff = new Date(a.inspectionDate).getTime() - new Date(b.inspectionDate).getTime();
        return this.sortDir === 'asc' ? diff : -diff;
      });
    } else {
      filtered = filtered.slice().sort((a, b) =>
        new Date(b.inspectionDate).getTime() - new Date(a.inspectionDate).getTime());
    }

    this.filteredEdls = filtered;
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

  getSignatureTone(edl: EtatDesLieux): string {
    if (edl.ownerSigned && edl.tenantSigned) return 'success';
    if (edl.ownerSigned || edl.tenantSigned) return 'warning';
    return 'neutral';
  }
}
