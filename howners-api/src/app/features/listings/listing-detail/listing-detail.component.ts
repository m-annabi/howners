import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Listing, LISTING_STATUS_LABELS, LISTING_STATUS_COLORS } from '../../../core/models/listing.model';
import { AMENITIES_MAP, REQUIREMENTS_MAP, AmenityItem } from '../../../core/models/listing-amenities.model';
import { Role } from '../../../core/models/user.model';

@Component({
  selector: 'app-listing-detail',
  templateUrl: './listing-detail.component.html'
})
export class ListingDetailComponent implements OnInit, OnDestroy {
  private userSub?: Subscription;
  listing: Listing | null = null;
  loading = true;
  isOwner = false;
  isAuthenticated = false;
  currentUserId: string | null = null;
  currentUserRole: Role | null = null;

  statusLabels = LISTING_STATUS_LABELS;
  statusColors = LISTING_STATUS_COLORS;
  amenitiesMap = AMENITIES_MAP;
  requirementsMap = REQUIREMENTS_MAP;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private listingService: ListingService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUserId = user?.id || null;
      this.currentUserRole = user?.role || null;
      this.isAuthenticated = !!user;
      if (this.listing) {
        this.isOwner = this.listing.ownerId === this.currentUserId;
      }
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadListing(id);
    }
  }

  loadListing(id: string): void {
    this.listingService.getListing(id).subscribe({
      next: (listing) => {
        this.listing = listing;
        this.isOwner = listing.ownerId === this.currentUserId;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/listings']);
      }
    });
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  get isTenant(): boolean {
    return this.currentUserRole === Role.TENANT;
  }

  get isRoleOwner(): boolean {
    return this.currentUserRole === Role.OWNER;
  }

  getAmenityLabel(key: string): string {
    return this.amenitiesMap[key]?.label || key;
  }

  getAmenityIcon(key: string): string {
    return this.amenitiesMap[key]?.icon || 'bi-check';
  }

  getRequirementLabel(key: string): string {
    return this.requirementsMap[key]?.label || key;
  }

  getRequirementIcon(key: string): string {
    return this.requirementsMap[key]?.icon || 'bi-exclamation-circle';
  }

  apply(): void {
    if (this.listing) {
      this.router.navigate(['/applications/new'], { queryParams: { listingId: this.listing.id } });
    }
  }

  contactOwner(): void {
    if (this.listing) {
      this.router.navigate(['/messages/new'], {
        queryParams: { recipientId: this.listing.ownerId, listingId: this.listing.id }
      });
    }
  }
}
