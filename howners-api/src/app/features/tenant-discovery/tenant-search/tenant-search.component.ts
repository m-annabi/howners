import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TenantDiscoveryService, TenantDiscoveryFilters } from '../../../core/services/tenant-discovery.service';
import { InvitationService } from '../../../core/services/invitation.service';
import { MessageService } from '../../../core/services/message.service';
import { ListingService } from '../../../core/services/listing.service';
import { TenantSearchResult } from '../../../core/models/tenant-search-result.model';
import { Listing } from '../../../core/models/listing.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { RISK_LEVEL_COLORS } from '../../../core/models/tenant-score.model';
import { Department, getDepartmentsByCountry, getDepartmentLabel } from '../../../core/data/geo-reference';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

@Component({
  selector: 'app-tenant-search',
  templateUrl: './tenant-search.component.html',
  styleUrls: ['./tenant-search.component.scss']
})
export class TenantSearchComponent implements OnInit, OnDestroy {
  private successTimeout: any;
  results: TenantSearchResult[] = [];
  myListings: Listing[] = [];
  loading = false;
  showMoreFilters = false;

  // Primary filters
  filterCity = '';
  filterDepartment = '';
  filterPostalCode = '';
  filterBudgetMin: number | null = null;
  filterBudgetMax: number | null = null;
  filterPropertyType = '';
  filterListingId = '';
  sortBy = '';

  // Quick-filter chip by property type
  activeTypeChip: string = 'all';

  departments: Department[] = [];
  getDepartmentLabel = getDepartmentLabel;
  propertyTypes = Object.values(PropertyType);
  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  riskLevelColors = RISK_LEVEL_COLORS;

  // Invitation
  invitingProfile: TenantSearchResult | null = null;
  inviteMessage = '';
  inviteListingId = '';
  inviteSuccess = '';
  inviteError = '';

  constructor(
    private discoveryService: TenantDiscoveryService,
    private invitationService: InvitationService,
    private listingService: ListingService,
    private messageService: MessageService,
    private router: Router
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
      },
      error: () => {}
    });
  }

  get typeFilterChips(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('all', this.results.length);
    for (const r of this.results) {
      const t = r.profile.desiredPropertyType || '__unspec';
      counts.set(t, (counts.get(t) || 0) + 1);
    }
    const list: QuickFilter[] = [
      { key: 'all', label: 'Tous', count: counts.get('all') || 0 }
    ];
    for (const t of this.propertyTypes) {
      const c = counts.get(t) || 0;
      if (c > 0) {
        list.push({ key: t, label: this.propertyTypeLabels[t], count: c });
      }
    }
    return list;
  }

  /** Client-side narrowing using the chip — server search already applies filterPropertyType when set. */
  get displayedResults(): TenantSearchResult[] {
    let list = this.activeTypeChip === 'all'
      ? this.results
      : this.results.filter(r => r.profile.desiredPropertyType === this.activeTypeChip);

    if (this.sortBy === 'profitability') {
      list = list.slice().sort((a, b) =>
        this.estimatedRevenue(b) - this.estimatedRevenue(a));
    }
    return list;
  }

  /**
   * Revenu mensuel estimé pondéré par la fiabilité (et la compatibilité si une
   * annonce de référence est sélectionnée). Sert au tri "Rentabilité estimée".
   */
  estimatedRevenue(result: TenantSearchResult): number {
    const budget = result.profile.budgetMax || result.profile.budgetMin || 0;
    if (budget <= 0) return 0;

    const listing = this.filterListingId
      ? this.myListings.find(l => l.id === this.filterListingId)
      : null;
    const target = listing?.pricePerMonth ? Math.min(budget, listing.pricePerMonth) : budget;

    const reliability = result.tenantScore?.score != null ? result.tenantScore.score / 100 : 0.5;
    const compat = result.compatibilityScore != null ? result.compatibilityScore / 100 : 1;
    return Math.round(target * reliability * compat);
  }

  onTypeChipChange(key: string): void {
    this.activeTypeChip = key;
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

  toggleMoreFilters(): void {
    this.showMoreFilters = !this.showMoreFilters;
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
    this.activeTypeChip = 'all';
    this.search();
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterCity || this.filterDepartment || this.filterPostalCode
      || this.filterBudgetMin != null || this.filterBudgetMax != null
      || this.filterPropertyType || this.filterListingId
      || this.activeTypeChip !== 'all');
  }

  openInviteModal(result: TenantSearchResult, event: Event): void {
    event.stopPropagation();
    this.invitingProfile = result;
    this.inviteMessage = '';
    this.inviteListingId = this.filterListingId || (this.myListings.length > 0 ? this.myListings[0].id : '');
    this.inviteSuccess = '';
    this.inviteError = '';
  }

  closeInviteModal(): void {
    this.invitingProfile = null;
    this.inviteError = '';
  }

  sendInvitation(): void {
    if (!this.invitingProfile || !this.inviteMessage.trim()) return;
    this.inviteError = '';
    const tenantId = this.invitingProfile.profile.tenantId;
    this.messageService.send({
      recipientId: tenantId,
      body: this.inviteMessage.trim(),
      ...(this.inviteListingId ? { listingId: this.inviteListingId } : {})
    }).subscribe({
      next: () => {
        this.invitingProfile = null;
        this.router.navigate(['/messages', tenantId]);
      },
      error: (err) => {
        this.inviteError = err.error?.message || 'Erreur lors de l\'envoi du message.';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.successTimeout) clearTimeout(this.successTimeout);
  }

  getCompatibilityColor(score: number): string {
    if (score >= 70) return 'success';
    if (score >= 40) return 'warning';
    return 'danger';
  }
}
