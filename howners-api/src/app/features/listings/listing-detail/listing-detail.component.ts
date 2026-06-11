import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { AuthService } from '../../../core/auth/auth.service';
import { SeoService } from '../../../core/services/seo.service';
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
    private authService: AuthService,
    private seoService: SeoService
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
        this.updateSeoForListing(listing);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/listings']);
      }
    });
  }

  /** Met à jour les méta-tags SEO en fonction de l'annonce chargée. */
  private updateSeoForListing(listing: Listing): void {
    const pricePart = listing.pricePerMonth ? `${listing.pricePerMonth} €/mois` : '';
    const locationPart = listing.propertyCity || '';
    const title = `${listing.title} — ${[locationPart, pricePart].filter(Boolean).join(' · ')} | Howners`;

    const descriptionRaw = listing.description || '';
    const description = descriptionRaw.length > 160 ? descriptionRaw.substring(0, 157) + '...' : descriptionRaw;

    const url = `${window.location.origin}/listings/${listing.id}`;
    const image = listing.photos?.length > 0 ? listing.photos[0].fileUrl : undefined;

    this.seoService.setMetaTags({ title, description, url, image });
    this.seoService.setCanonical(url);
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

  // ---- Share -----------------------------------------------------------

  shareSuccess = false;

  async shareListing(): Promise<void> {
    if (!this.listing) return;
    const url = `${window.location.origin}/listings/${this.listing.id}`;
    const title = this.listing.title || 'Annonce Howners';
    const text = `${title} — ${this.listing.propertyCity || ''} · ${this.listing.pricePerMonth || ''} €/mois`;

    // Web Share API (mobile + Safari/Chrome modernes)
    const nav = navigator as any;
    if (nav.share) {
      try {
        await nav.share({ title, text, url });
        return;
      } catch (e) {
        // user cancelled; fall through to copy
      }
    }

    // Fallback: copy link to clipboard
    try {
      await navigator.clipboard.writeText(url);
      this.shareSuccess = true;
      setTimeout(() => this.shareSuccess = false, 2500);
    } catch {
      // last-resort: open mailto
      window.location.href = `mailto:?subject=${encodeURIComponent(title)}&body=${encodeURIComponent(text + '\n\n' + url)}`;
    }
  }
}
