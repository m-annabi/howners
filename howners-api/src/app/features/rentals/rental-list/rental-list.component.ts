import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RentalService } from '../rental.service';
import { Rental, RENTAL_STATUS_LABELS, RENTAL_STATUS_COLORS, RentalStatus } from '../../../core/models/rental.model';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

@Component({
  selector: 'app-rental-list',
  templateUrl: './rental-list.component.html',
  styleUrls: ['./rental-list.component.scss']
})
export class RentalListComponent implements OnInit {
  rentals: Rental[] = [];
  filteredRentals: Rental[] = [];
  filters: QuickFilter[] = [];
  loading = true;
  error: string | null = null;
  searchTerm = '';
  filterStatus: string = 'all';
  sortCol = '';
  sortDir: 'asc' | 'desc' = 'desc';

  rentalStatusLabels = RENTAL_STATUS_LABELS;
  rentalStatusColors = RENTAL_STATUS_COLORS;

  get isTenant(): boolean {
    return this.authService.hasRole('TENANT');
  }

  constructor(
    private rentalService: RentalService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const f = params['filter'];
      if (f && Object.values(RentalStatus).includes(f as RentalStatus)) {
        this.filterStatus = f;
      }
    });
    this.loadRentals();
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

  private buildFilters(): void {
    const counts = new Map<string, number>();
    counts.set('all', this.rentals.length);
    for (const r of this.rentals) {
      counts.set(r.status, (counts.get(r.status) || 0) + 1);
    }
    const list: QuickFilter[] = [
      { key: 'all', label: 'Toutes', count: counts.get('all') || 0 },
      { key: RentalStatus.VACANT, label: 'Libres', count: counts.get(RentalStatus.VACANT) || 0 },
      { key: RentalStatus.LISTED, label: 'En annonce', count: counts.get(RentalStatus.LISTED) || 0 },
      { key: RentalStatus.ACTIVE, label: 'Actives', count: counts.get(RentalStatus.ACTIVE) || 0, tone: 'success' },
      { key: RentalStatus.EXITING, label: 'Sortie prog.', count: counts.get(RentalStatus.EXITING) || 0, tone: 'warning' },
      { key: RentalStatus.PENDING, label: 'En attente', count: counts.get(RentalStatus.PENDING) || 0, tone: 'warning' },
      { key: RentalStatus.TERMINATED, label: 'Terminées', count: counts.get(RentalStatus.TERMINATED) || 0 },
    ];
    this.filters = list.filter(f => f.key === 'all' || (f.count || 0) > 0);
  }

  loadRentals(): void {
    this.loading = true;
    this.error = null;

    this.rentalService.getRentals().subscribe({
      next: (page) => {
        this.rentals = page.content;
        this.buildFilters();
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des locations';
        this.loading = false;
      }
    });
  }

  onFilterChange(key: string): void {
    this.filterStatus = key;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.rentals;

    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(rental => rental.status === this.filterStatus);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(rental =>
        rental.propertyName.toLowerCase().includes(term) ||
        (rental.tenantName && rental.tenantName.toLowerCase().includes(term)) ||
        (rental.tenantEmail && rental.tenantEmail.toLowerCase().includes(term))
      );
    }

    if (this.sortCol) {
      filtered = filtered.slice().sort((a, b) => {
        let diff = 0;
        if (this.sortCol === 'date') diff = new Date(a.startDate ?? 0).getTime() - new Date(b.startDate ?? 0).getTime();
        else if (this.sortCol === 'rent') diff = (a.monthlyRent ?? 0) - (b.monthlyRent ?? 0);
        else if (this.sortCol === 'tenant') diff = (a.tenantName || '').localeCompare(b.tenantName || '');
        else if (this.sortCol === 'property') diff = a.propertyName.localeCompare(b.propertyName);
        else if (this.sortCol === 'status') diff = a.status.localeCompare(b.status);
        return this.sortDir === 'asc' ? diff : -diff;
      });
    } else {
      filtered = filtered.slice().sort((a, b) =>
        new Date(b.startDate ?? 0).getTime() - new Date(a.startDate ?? 0).getTime());
    }

    this.filteredRentals = filtered;
  }

  viewRental(id: string): void {
    this.router.navigate(['/rentals', id]);
  }

  editRental(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/rentals', id, 'edit']);
  }

  viewPayments(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/payments'], { queryParams: { rentalId: id } });
  }

  deleteRental(id: string, event: Event): void {
    event.stopPropagation();

    if (!confirm('Êtes-vous sûr de vouloir supprimer cette location ?')) {
      return;
    }

    this.rentalService.deleteRental(id).subscribe({
      next: () => {
        this.rentals = this.rentals.filter(r => r.id !== id);
        this.applyFilters();
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  navigateToCreate(): void {
    this.router.navigate(['/rentals/new']);
  }

  getStatusColor(status: RentalStatus): string {
    return this.rentalStatusColors[status];
  }
}
