import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RentalService } from '../rental.service';
import { Rental, RENTAL_TYPE_LABELS, RENTAL_STATUS_LABELS, RENTAL_STATUS_COLORS, RentalStatus } from '../../../core/models/rental.model';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-rental-list',
  templateUrl: './rental-list.component.html',
  styleUrls: ['./rental-list.component.scss']
})
export class RentalListComponent implements OnInit {
  rentals: Rental[] = [];
  filteredRentals: Rental[] = [];
  loading = true;
  error: string | null = null;
  searchTerm = '';
  filterStatus: string = 'all';

  rentalTypeLabels = RENTAL_TYPE_LABELS;
  rentalStatusLabels = RENTAL_STATUS_LABELS;
  rentalStatusColors = RENTAL_STATUS_COLORS;

  statuses = Object.keys(RentalStatus).map(key => ({
    value: RentalStatus[key as keyof typeof RentalStatus],
    label: RENTAL_STATUS_LABELS[RentalStatus[key as keyof typeof RentalStatus]]
  }));

  constructor(
    private rentalService: RentalService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadRentals();
  }

  loadRentals(): void {
    this.loading = true;
    this.error = null;

    this.rentalService.getRentals().subscribe({
      next: (rentals) => {
        this.rentals = rentals;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des locations';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.rentals;

    // Filtrer par statut
    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(rental => rental.status === this.filterStatus);
    }

    // Filtrer par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(rental =>
        rental.propertyName.toLowerCase().includes(term) ||
        (rental.tenantName && rental.tenantName.toLowerCase().includes(term)) ||
        (rental.tenantEmail && rental.tenantEmail.toLowerCase().includes(term))
      );
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
