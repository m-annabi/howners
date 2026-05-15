import { Component, OnInit } from '@angular/core';
import { ListingService } from '../../../core/services/listing.service';
import { Listing, LISTING_STATUS_LABELS } from '../../../core/models/listing.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { COUNTRIES, Department, getDepartmentsByCountry, getDepartmentLabel } from '../../../core/data/geo-reference';
import { GeolocationService } from '../../../core/services/geolocation.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-listing-search',
  templateUrl: './listing-search.component.html',
  styleUrls: ['./listing-search.component.scss']
})
export class ListingSearchComponent implements OnInit {
  listings: Listing[] = [];
  searchQuery = '';
  loading = false;
  showMoreFilters = false;
  geolocating = false;
  detectedLocationLabel: string | null = null;

  // Primary location
  filterCity = '';
  filterPostalCode = '';
  filterCountry = '';
  filterDepartment = '';

  // Primary price/surface/type
  filterPriceMin: number | null = null;
  filterPriceMax: number | null = null;
  filterMinSurface: number | null = null;
  filterPropertyType = '';

  // Secondary
  filterMinBedrooms: number | null = null;
  filterFurnished: string = '';
  filterAvailableFrom = '';
  sortBy = '';

  countries = COUNTRIES;
  filteredDepartments: Department[] = [];
  getDepartmentLabel = getDepartmentLabel;
  propertyTypes = Object.values(PropertyType);
  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  statusLabels = LISTING_STATUS_LABELS;

  constructor(
    private listingService: ListingService,
    private geolocationService: GeolocationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateDepartments();
    this.search();
  }

  search(): void {
    this.loading = true;
    const filters: any = {};
    if (this.searchQuery) filters.search = this.searchQuery;
    if (this.filterCity) filters.city = this.filterCity;
    if (this.filterDepartment) filters.department = this.filterDepartment;
    if (this.filterPostalCode) filters.postalCode = this.filterPostalCode;
    if (this.filterPriceMin != null) filters.priceMin = this.filterPriceMin;
    if (this.filterPriceMax != null) filters.priceMax = this.filterPriceMax;
    if (this.filterPropertyType) filters.propertyType = this.filterPropertyType;
    if (this.filterMinSurface != null) filters.minSurface = this.filterMinSurface;
    if (this.filterMinBedrooms != null) filters.minBedrooms = this.filterMinBedrooms;
    if (this.filterFurnished === 'true') filters.furnished = true;
    if (this.filterFurnished === 'false') filters.furnished = false;
    if (this.filterAvailableFrom) filters.availableFrom = this.filterAvailableFrom;
    if (this.sortBy) filters.sortBy = this.sortBy;

    this.listingService.searchListings(Object.keys(filters).length > 0 ? filters : undefined).subscribe({
      next: (listings) => {
        this.listings = listings;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  toggleMoreFilters(): void {
    this.showMoreFilters = !this.showMoreFilters;
  }

  /**
   * Détecte la position de l'utilisateur via navigator.geolocation,
   * convertit en adresse et pré-remplit le filtre code postal (+ ville comme indication).
   */
  locateMe(): void {
    if (this.geolocating) return;
    this.geolocating = true;
    this.detectedLocationLabel = null;

    this.geolocationService.detectUserLocation().subscribe({
      next: (result) => {
        this.geolocating = false;
        if (result.postalCode) {
          this.filterPostalCode = result.postalCode;
        } else if (result.city) {
          this.filterCity = result.city;
        }
        const labelParts = [result.city, result.postalCode].filter(Boolean);
        this.detectedLocationLabel = labelParts.length > 0
          ? `Position détectée : ${labelParts.join(' · ')}`
          : 'Position détectée';
        this.notificationService.success(this.detectedLocationLabel);
        this.search();
      },
      error: (err) => {
        this.geolocating = false;
        this.notificationService.error(err?.message || 'Impossible de récupérer votre position.');
      }
    });
  }

  onCountryFilterChange(): void {
    this.updateDepartments();
    this.filterDepartment = '';
  }

  clearFilters(): void {
    this.filterCity = '';
    this.filterDepartment = '';
    this.filterPostalCode = '';
    this.filterCountry = '';
    this.filterPriceMin = null;
    this.filterPriceMax = null;
    this.filterPropertyType = '';
    this.filterMinSurface = null;
    this.filterMinBedrooms = null;
    this.filterFurnished = '';
    this.filterAvailableFrom = '';
    this.sortBy = '';
    this.detectedLocationLabel = null;
    this.updateDepartments();
    this.search();
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterCity || this.filterDepartment || this.filterPostalCode
      || this.filterPriceMin != null || this.filterPriceMax != null
      || this.filterPropertyType || this.filterMinSurface != null
      || this.filterMinBedrooms != null || this.filterFurnished
      || this.filterAvailableFrom);
  }

  private updateDepartments(): void {
    if (this.filterCountry) {
      this.filteredDepartments = getDepartmentsByCountry(this.filterCountry);
    } else {
      this.filteredDepartments = getDepartmentsByCountry('FR');
    }
  }
}
