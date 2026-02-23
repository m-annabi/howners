import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PropertyService } from '../property.service';
import { Property, PROPERTY_TYPE_LABELS, HEATING_TYPE_LABELS, PROPERTY_CONDITION_LABELS, DPE_COLORS, GES_COLORS, HeatingType, PropertyCondition } from '../../../core/models/property.model';
import { NotificationService } from '../../../core/services/notification.service';
import { RentalService } from '../../rentals/rental.service';
import { Rental, RentalStatus } from '../../../core/models/rental.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-property-detail',
  templateUrl: './property-detail.component.html',
  styleUrls: ['./property-detail.component.scss']
})
export class PropertyDetailComponent implements OnInit {
  property: Property | null = null;
  activeRental: Rental | null = null;
  monthlyRent: number = 0;
  loading = true;
  error: string | null = null;

  propertyTypeLabels = PROPERTY_TYPE_LABELS;

  constructor(
    private propertyService: PropertyService,
    private rentalService: RentalService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPropertyAndRentals(id);
    }
  }

  loadPropertyAndRentals(id: string): void {
    this.loading = true;

    forkJoin({
      property: this.propertyService.getProperty(id),
      rentals: this.rentalService.getRentals()
    }).subscribe({
      next: ({ property, rentals }) => {
        this.property = property;

        // Find active rental for this property
        this.activeRental = rentals.find(
          r => r.propertyId === id && r.status === RentalStatus.ACTIVE
        ) || null;

        // Set monthly rent for profitability component
        this.monthlyRent = this.activeRental?.monthlyRent || 0;

        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du bien';
        this.loading = false;
      }
    });
  }

  loadProperty(id: string): void {
    this.loading = true;
    this.propertyService.getProperty(id).subscribe({
      next: (property) => {
        this.property = property;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du bien';
        this.loading = false;
      }
    });
  }

  editProperty(): void {
    if (this.property) {
      this.router.navigate(['/properties', this.property.id, 'edit']);
    }
  }

  deleteProperty(): void {
    if (!this.property) return;

    if (!confirm('Êtes-vous sûr de vouloir supprimer ce bien ?')) {
      return;
    }

    this.propertyService.deleteProperty(this.property.id).subscribe({
      next: () => {
        this.router.navigate(['/properties']);
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/properties']);
  }

  viewRentals(): void {
    if (this.property) {
      this.router.navigate(['/rentals'], {
        queryParams: { propertyId: this.property.id }
      });
    }
  }

  viewDocuments(): void {
    const el = document.querySelector('app-document-list');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  viewStats(): void {
    if (this.property) {
      this.router.navigate(['/financial'], {
        queryParams: { propertyId: this.property.id }
      });
    }
  }

  viewRentalDetail(): void {
    if (this.activeRental) {
      this.router.navigate(['/rentals', this.activeRental.id]);
    }
  }

  hasTechnicalInfo(): boolean {
    if (!this.property) return false;
    return !!(
      this.property.dpeRating ||
      this.property.gesRating ||
      this.property.constructionYear ||
      this.property.floorNumber !== null ||
      this.property.totalFloors ||
      this.property.heatingType ||
      this.property.hasParking !== null ||
      this.property.hasElevator !== null ||
      this.property.isFurnished !== null ||
      this.property.propertyCondition
    );
  }

  getDpeColor(rating: string): string {
    return DPE_COLORS[rating] || '#999';
  }

  getGesColor(rating: string): string {
    return GES_COLORS[rating] || '#999';
  }

  getHeatingTypeLabel(type: HeatingType): string {
    return HEATING_TYPE_LABELS[type] || type;
  }

  getPropertyConditionLabel(condition: PropertyCondition): string {
    return PROPERTY_CONDITION_LABELS[condition] || condition;
  }
}
