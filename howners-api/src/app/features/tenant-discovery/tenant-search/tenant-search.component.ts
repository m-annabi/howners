import { Component, OnInit } from '@angular/core';
import { TenantDiscoveryService, TenantDiscoveryFilters } from '../../../core/services/tenant-discovery.service';
import { InvitationService } from '../../../core/services/invitation.service';
import { ListingService } from '../../../core/services/listing.service';
import { TenantSearchResult } from '../../../core/models/tenant-search-result.model';
import { Listing } from '../../../core/models/listing.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { RISK_LEVEL_COLORS } from '../../../core/models/tenant-score.model';
import { Department, getDepartmentsByCountry, getDepartmentLabel } from '../../../core/data/geo-reference';

@Component({
  selector: 'app-tenant-search',
  templateUrl: './tenant-search.component.html',
  styleUrls: ['./tenant-search.component.scss']
})
export class TenantSearchComponent implements OnInit {
  results: TenantSearchResult[] = [];
  myListings: Listing[] = [];
  loading = false;
  showFilters = true;

  // Filters
  filterCity = '';
  filterDepartment = '';
  filterPostalCode = '';
  filterBudgetMin: number | null = null;
  filterBudgetMax: number | null = null;
  filterPropertyType = '';
  filterListingId = '';
  sortBy = '';

  departments: Department[] = [];
  getDepartmentLabel = getDepartmentLabel;
  propertyTypes = Object.values(PropertyType);
  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  riskLevelColors = RISK_LEVEL_COLORS;

  // Invitation
  invitingProfileId: string | null = null;
  inviteMessage = '';
  inviteListingId = '';
  inviteSuccess = '';
  inviteError = '';

  constructor(
    private discoveryService: TenantDiscoveryService,
    private invitationService: InvitationService,
    private listingService: ListingService
  ) {}

  ngOnInit(): void {
    this.departments = getDepartmentsByCountry('FR');
    this.loadMyListings();
    this.search();
  }

  loadMyListings(): void {
    this.listingService.getMyListings().subscribe({
      next: (listings) => {
        this.myListings = listings.filter(l => l.status === 'PUBLISHED');
      }
    });
  }

  search(): void {
    this.loading = true;
    const filters: TenantDiscoveryFilters = {};
    if (this.filterCity) filters.city = this.filterCity;
    if (this.filterDepartment) filters.department = this.filterDepartment;
    if (this.filterPostalCode) filters.postalCode = this.filterPostalCode;
    if (this.filterBudgetMin != null) filters.budgetMin = this.filterBudgetMin;
    if (this.filterBudgetMax != null) filters.budgetMax = this.filterBudgetMax;
    if (this.filterPropertyType) filters.propertyType = this.filterPropertyType;
    if (this.filterListingId) filters.listingId = this.filterListingId;
    if (this.sortBy) filters.sortBy = this.sortBy;

    this.discoveryService.searchTenants(Object.keys(filters).length > 0 ? filters : undefined).subscribe({
      next: (results) => {
        this.results = results;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  clearFilters(): void {
    this.filterCity = '';
    this.filterDepartment = '';
    this.filterPostalCode = '';
    this.filterBudgetMin = null;
    this.filterBudgetMax = null;
    this.filterPropertyType = '';
    this.filterListingId = '';
    this.sortBy = '';
    this.search();
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterCity || this.filterDepartment || this.filterPostalCode
      || this.filterBudgetMin != null || this.filterBudgetMax != null
      || this.filterPropertyType || this.filterListingId);
  }

  openInviteModal(profileId: string): void {
    this.invitingProfileId = profileId;
    this.inviteMessage = '';
    this.inviteListingId = this.filterListingId || (this.myListings.length > 0 ? this.myListings[0].id : '');
    this.inviteSuccess = '';
    this.inviteError = '';
  }

  cancelInvite(): void {
    this.invitingProfileId = null;
  }

  sendInvitation(tenantId: string): void {
    if (!this.inviteListingId) {
      this.inviteError = 'Veuillez sélectionner une annonce.';
      return;
    }
    this.inviteError = '';
    this.invitationService.invite({
      listingId: this.inviteListingId,
      tenantId: tenantId,
      message: this.inviteMessage || undefined
    }).subscribe({
      next: () => {
        this.inviteSuccess = 'Invitation envoyée !';
        this.invitingProfileId = null;
        setTimeout(() => this.inviteSuccess = '', 3000);
      },
      error: (err) => {
        this.inviteError = err.error?.message || 'Erreur lors de l\'envoi de l\'invitation.';
      }
    });
  }

  getCompatibilityColor(score: number): string {
    if (score >= 70) return 'success';
    if (score >= 40) return 'warning';
    return 'danger';
  }
}
