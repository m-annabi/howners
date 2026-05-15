import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, filter } from 'rxjs/operators';
import { PropertyService } from '../property.service';
import { Property, PROPERTY_TYPE_LABELS, PropertyType } from '../../../core/models/property.model';
import { PropertyPhotoService } from '../../../core/services/property-photo.service';
import { PropertyPhotoStateService } from '../../../core/services/property-photo-state.service';
import { NotificationService } from '../../../core/services/notification.service';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

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
  activeFilter: string = 'all';
  sortBy: 'default' | 'yield_desc' | 'yield_asc' | 'rent_desc' = 'default';

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

    this.photoUpdateSubscription = this.photoStateService.photoUpdated$.subscribe(
      propertyId => this.refreshPropertyPhoto(propertyId)
    );

    this.navigationSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      filter((event) => event.urlAfterRedirects === '/properties')
    ).subscribe(() => {
      if (this.properties.length > 0) {
        this.loadPrimaryPhotos();
      }
    });
  }

  ngOnDestroy(): void {
    this.photoUpdateSubscription?.unsubscribe();
    this.navigationSubscription?.unsubscribe();
  }

  get filters(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('all', this.properties.length);
    for (const p of this.properties) {
      counts.set(p.propertyType, (counts.get(p.propertyType) || 0) + 1);
    }
    const base: QuickFilter[] = [
      { key: 'all', label: 'Tous', count: counts.get('all') || 0 }
    ];
    const types: PropertyType[] = [
      PropertyType.APARTMENT, PropertyType.HOUSE, PropertyType.STUDIO,
      PropertyType.DUPLEX, PropertyType.VILLA, PropertyType.LOFT, PropertyType.OTHER
    ];
    for (const t of types) {
      const c = counts.get(t) || 0;
      if (c > 0) {
        base.push({ key: t, label: this.propertyTypeLabels[t], count: c });
      }
    }
    return base;
  }

  loadProperties(): void {
    this.loading = true;
    this.error = null;

    this.propertyService.getProperties().subscribe({
      next: (properties) => {
        this.properties = properties;
        this.applyFilters();
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
      this.photoService.getPrimaryPhoto(property.id).pipe(catchError(() => of(null)))
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
      error: () => {
        this.loading = false;
      }
    });
  }

  refreshPropertyPhoto(propertyId: string): void {
    const property = this.properties.find(p => p.id === propertyId);
    if (!property) return;

    this.photoService.getPrimaryPhoto(propertyId).pipe(catchError(() => of(null)))
      .subscribe(photo => {
        if (photo) {
          property.primaryPhotoUrl = photo.fileUrl;
        } else {
          delete property.primaryPhotoUrl;
        }
      });
  }

  onFilterChange(key: string): void {
    this.activeFilter = key;
    this.applyFilters();
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    let list = this.properties.filter(p => {
      const matchType = this.activeFilter === 'all' || p.propertyType === this.activeFilter;
      const matchSearch = !term ||
        p.name.toLowerCase().includes(term) ||
        p.address.city.toLowerCase().includes(term) ||
        p.address.postalCode.includes(term);
      return matchType && matchSearch;
    });

    // Sort
    if (this.sortBy === 'yield_desc' || this.sortBy === 'yield_asc') {
      const sign = this.sortBy === 'yield_desc' ? -1 : 1;
      list = list.slice().sort((a, b) => {
        const ya = a.grossYieldPercent ?? -Infinity;
        const yb = b.grossYieldPercent ?? -Infinity;
        return sign * (ya - yb);
      });
    } else if (this.sortBy === 'rent_desc') {
      list = list.slice().sort((a, b) =>
        (b.currentMonthlyRent ?? -1) - (a.currentMonthlyRent ?? -1));
    }

    this.filteredProperties = list;
  }

  onSortChange(): void {
    this.applyFilters();
  }

  filterProperties(): void {
    this.applyFilters();
  }

  viewProperty(id: string): void {
    this.router.navigate(['/properties', id]);
  }

  editProperty(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/properties', id, 'edit']);
  }

  viewProfitability(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/properties', id, 'profitability']);
  }

  deleteProperty(id: string, event: Event): void {
    event.stopPropagation();

    if (!confirm('Êtes-vous sûr de vouloir supprimer ce bien ?')) {
      return;
    }

    this.propertyService.deleteProperty(id).subscribe({
      next: () => {
        this.properties = this.properties.filter(p => p.id !== id);
        this.applyFilters();
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
