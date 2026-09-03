import { NavigationService } from '../../../core/services/navigation.service';
import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PropertyService } from '../property.service';
import { Property, PROPERTY_TYPE_LABELS, HEATING_TYPE_LABELS, PROPERTY_CONDITION_LABELS, DPE_COLORS, GES_COLORS, HeatingType, PropertyCondition } from '../../../core/models/property.model';
import { NotificationService } from '../../../core/services/notification.service';
import { RentalService } from '../../rentals/rental.service';
import { Rental, RentalStatus } from '../../../core/models/rental.model';
import { ListingService } from '../../../core/services/listing.service';
import { Listing, ListingStatus } from '../../../core/models/listing.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

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
  // Annonces de CE bien (hors clôturées) — pilote le bandeau « publiez votre annonce ».
  listings: Listing[] = [];

  propertyTypeLabels = PROPERTY_TYPE_LABELS;

  constructor(
    private propertyService: PropertyService,
    private rentalService: RentalService,
    private listingService: ListingService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private confirmDialog: ConfirmDialogService,
    private nav: NavigationService
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
      rentalsPage: this.rentalService.getRentals(),
      myListings: this.listingService.getMyListings(0, 200).pipe(catchError(() => of({ content: [] as Listing[] })))
    }).subscribe({
      next: ({ property, rentalsPage, myListings }) => {
        this.property = property;

        // Find active rental for this property
        this.activeRental = rentalsPage.content.find(
          r => r.propertyId === id && r.status === RentalStatus.ACTIVE
        ) || null;

        // Set monthly rent for profitability component
        this.monthlyRent = this.activeRental?.monthlyRent || 0;

        // Annonces vivantes de ce bien (les clôturées ne comptent pas)
        this.listings = (myListings.content || []).filter(
          l => l.propertyId === id && l.status !== ListingStatus.CLOSED
        );

        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du bien';
        this.loading = false;
      }
    });
  }

  get publishedListing(): Listing | null {
    return this.listings.find(l => l.status === ListingStatus.PUBLISHED || l.status === ListingStatus.PAUSED) || null;
  }

  get draftListing(): Listing | null {
    return this.listings.find(l => l.status === ListingStatus.DRAFT) || null;
  }

  /** Bien sans annonce ni location active : la prochaine étape est de publier. */
  get showPublishCta(): boolean {
    return !this.loading && !this.activeRental && this.listings.length === 0;
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

    this.confirmDialog.confirm('Confirmer la suppression', 'Êtes-vous sûr de vouloir supprimer ce bien ?', 'danger').subscribe(ok => {
      if (!ok) return;

      this.propertyService.deleteProperty(this.property!.id).subscribe({
        next: () => {
          this.router.navigate(['/properties']);
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
        }
      });
    });
  }

  goBack(): void {
    this.nav.back(['/properties']);
  }

  /** Enchaînement naturel bien → annonce : le formulaire arrive avec le bien présélectionné. */
  publishListing(): void {
    if (this.property) {
      this.router.navigate(['/listings/new'], { queryParams: { propertyId: this.property.id } });
    }
  }

  viewRentals(): void {
    if (this.property) {
      this.router.navigate(['/rentals'], {
        queryParams: { propertyId: this.property.id }
      });
    }
  }

  viewDocuments(): void {
    const docSection = document.querySelector('app-document-list');
    if (docSection) {
      docSection.scrollIntoView({ behavior: 'smooth' });
    }
  }

  viewStats(): void {
    this.router.navigate(['/financial']);
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
