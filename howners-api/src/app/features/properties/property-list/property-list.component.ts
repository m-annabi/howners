import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, filter } from 'rxjs/operators';
import { PropertyService } from '../property.service';
import { Property, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { PropertyPhotoService } from '../../../core/services/property-photo.service';
import { PropertyPhotoStateService } from '../../../core/services/property-photo-state.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-property-list',
  templateUrl: './property-list.component.html',
  styleUrls: ['./property-list.component.scss']
})
export class PropertyListComponent implements OnInit, OnDestroy {
  properties: Property[] = [];
  filteredProperties: Property[] = [];
  loading = true;
  error: string | null = null;
  searchTerm = '';

  propertyTypeLabels = PROPERTY_TYPE_LABELS;

  private photoUpdateSubscription?: Subscription;
  private navigationSubscription?: Subscription;

  constructor(
    private propertyService: PropertyService,
    private photoService: PropertyPhotoService,
    private photoStateService: PropertyPhotoStateService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadProperties();

    // S'abonner aux changements de photos
    this.photoUpdateSubscription = this.photoStateService.photoUpdated$.subscribe(
      propertyId => {
        this.refreshPropertyPhoto(propertyId);
      }
    );

    // Détecter la navigation pour recharger la liste quand on revient dessus
    this.navigationSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      filter((event) => event.urlAfterRedirects === '/properties')
    ).subscribe(() => {
      // Recharger uniquement les photos pour éviter de recharger toute la liste
      if (this.properties.length > 0) {
        this.loadPrimaryPhotos();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.photoUpdateSubscription) {
      this.photoUpdateSubscription.unsubscribe();
    }
    if (this.navigationSubscription) {
      this.navigationSubscription.unsubscribe();
    }
  }

  loadProperties(): void {
    this.loading = true;
    this.error = null;

    this.propertyService.getProperties().subscribe({
      next: (properties) => {
        this.properties = properties;
        this.filteredProperties = properties;
        this.loadPrimaryPhotos();
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des biens';
        this.loading = false;
      }
    });
  }

  loadPrimaryPhotos(): void {
    if (this.properties.length === 0) {
      this.loading = false;
      return;
    }

    const photoRequests = this.properties.map(property =>
      this.photoService.getPrimaryPhoto(property.id).pipe(
        catchError(() => of(null))
      )
    );

    forkJoin(photoRequests).subscribe({
      next: (photos) => {
        photos.forEach((photo, index) => {
          if (photo) {
            this.properties[index].primaryPhotoUrl = photo.fileUrl;
          }
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading primary photos:', err);
        this.loading = false;
      }
    });
  }

  refreshPropertyPhoto(propertyId: string): void {
    // Trouver la propriété dans la liste
    const property = this.properties.find(p => p.id === propertyId);
    if (!property) return;

    // Recharger la photo de couverture
    this.photoService.getPrimaryPhoto(propertyId).pipe(
      catchError(() => of(null))
    ).subscribe(photo => {
      if (photo) {
        property.primaryPhotoUrl = photo.fileUrl;
      } else {
        // Si pas de photo, supprimer l'URL
        delete property.primaryPhotoUrl;
      }
    });
  }

  filterProperties(): void {
    if (!this.searchTerm) {
      this.filteredProperties = this.properties;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredProperties = this.properties.filter(property =>
      property.name.toLowerCase().includes(term) ||
      property.address.city.toLowerCase().includes(term) ||
      property.address.postalCode.includes(term)
    );
  }

  viewProperty(id: string): void {
    this.router.navigate(['/properties', id]);
  }

  editProperty(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/properties', id, 'edit']);
  }

  deleteProperty(id: string, event: Event): void {
    event.stopPropagation();

    if (!confirm('Êtes-vous sûr de vouloir supprimer ce bien ?')) {
      return;
    }

    this.propertyService.deleteProperty(id).subscribe({
      next: () => {
        this.properties = this.properties.filter(p => p.id !== id);
        this.filterProperties();
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  navigateToCreate(): void {
    this.router.navigate(['/properties/new']);
  }
}
