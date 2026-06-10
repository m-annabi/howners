import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TenantDiscoveryService } from '../../../core/services/tenant-discovery.service';
import { InvitationService } from '../../../core/services/invitation.service';
import { ListingService } from '../../../core/services/listing.service';
import { TenantSearchResult } from '../../../core/models/tenant-search-result.model';
import { Listing } from '../../../core/models/listing.model';
import { PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { FURNISHED_PREFERENCE_LABELS } from '../../../core/models/tenant-search-profile.model';
import { RISK_LEVEL_LABELS, RISK_LEVEL_COLORS } from '../../../core/models/tenant-score.model';

@Component({
  selector: 'app-tenant-profile-detail',
  templateUrl: './tenant-profile-detail.component.html',
  styleUrls: ['./tenant-profile-detail.component.scss']
})
export class TenantProfileDetailComponent implements OnInit, OnDestroy {
  private successTimeout: any;
  result: TenantSearchResult | null = null;
  myListings: Listing[] = [];
  loading = false;
  profileId = '';
  selectedListingId = '';

  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  furnishedLabels = FURNISHED_PREFERENCE_LABELS;
  riskLevelLabels = RISK_LEVEL_LABELS;
  riskLevelColors = RISK_LEVEL_COLORS;

  // Invitation form
  inviteListingId = '';
  inviteMessage = '';
  inviteSending = false;
  inviteSuccess = '';
  inviteError = '';

  constructor(
    private route: ActivatedRoute,
    private discoveryService: TenantDiscoveryService,
    private invitationService: InvitationService,
    private listingService: ListingService
  ) {}

  ngOnInit(): void {
    this.profileId = this.route.snapshot.paramMap.get('profileId') || '';
    this.selectedListingId = this.route.snapshot.queryParamMap.get('listingId') || '';
    this.loadMyListings();
    this.loadProfile();
  }

  loadMyListings(): void {
    this.listingService.getMyListings().subscribe({
      next: (listings) => {
        this.myListings = listings.filter(l => l.status === 'PUBLISHED');
        if (!this.inviteListingId && this.myListings.length > 0) {
          this.inviteListingId = this.selectedListingId || this.myListings[0].id;
        }
      },
      error: () => {} // silent — listings dropdown stays empty
    });
  }

  loadProfile(): void {
    this.loading = true;
    this.discoveryService.getTenantProfile(this.profileId, this.selectedListingId || undefined).subscribe({
      next: (result) => {
        this.result = result;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  onListingChange(): void {
    this.selectedListingId = this.selectedListingId;
    this.loadProfile();
  }

  sendInvitation(): void {
    if (!this.inviteListingId || !this.result) return;
    this.inviteSending = true;
    this.inviteError = '';
    this.invitationService.invite({
      listingId: this.inviteListingId,
      tenantId: this.result.profile.tenantId,
      message: this.inviteMessage || undefined
    }).subscribe({
      next: () => {
        this.inviteSending = false;
        this.inviteSuccess = 'Invitation envoyée avec succès !';
        this.inviteMessage = '';
        this.successTimeout = setTimeout(() => this.inviteSuccess = '', 3000);
      },
      error: (err) => {
        this.inviteSending = false;
        this.inviteError = err.error?.message || 'Erreur lors de l\'envoi.';
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
