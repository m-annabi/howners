import { Component, OnInit } from '@angular/core';
import { ListingService } from '../../../core/services/listing.service';
import { Listing, LISTING_STATUS_LABELS } from '../../../core/models/listing.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { COUNTRIES, Department, getDepartmentsByCountry, getDepartmentLabel } from '../../../core/data/geo-reference';

@Component({
  selector: 'app-listing-search',
  templateUrl: './listing-search.component.html'
})
export class ListingSearchComponent implements OnInit {
  listings: Listing[] = [];
  searchQuery = '';
  loading = false;
  showFilters = false;

  // Location filters
  filterCity = '';
  filterDepartment = '';
  filterPostalCode = '';
  filterCountry = '';

  // Advanced filters
  filterPriceMin: number | null = null;
  filterPriceMax: number | null = null;
  filterPropertyType = '';
  filterMinSurface: number | null = null;
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

  constructor(private listingService: ListingService) {}

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

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
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
      // Show all departments when no country filter
      this.filteredDepartments = getDepartmentsByCountry('FR');
    }
  }
}
