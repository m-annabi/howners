import { Component, OnInit } from '@angular/core';
import { ListingService } from '../../../core/services/listing.service';
import { Listing, ListingStatus, LISTING_STATUS_LABELS, LISTING_STATUS_COLORS } from '../../../core/models/listing.model';

@Component({
  selector: 'app-my-listings',
  templateUrl: './my-listings.component.html'
})
export class MyListingsComponent implements OnInit {
  listings: Listing[] = [];
  loading = false;

  statusLabels = LISTING_STATUS_LABELS;
  statusColors = LISTING_STATUS_COLORS;

  constructor(private listingService: ListingService) {}

  ngOnInit(): void {
    this.loadListings();
  }

  loadListings(): void {
    this.loading = true;
    this.listingService.getMyListings().subscribe({
      next: (listings) => {
        this.listings = listings;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  publish(id: string): void {
    this.listingService.publishListing(id).subscribe(() => this.loadListings());
  }

  pause(id: string): void {
    this.listingService.pauseListing(id).subscribe(() => this.loadListings());
  }

  close(id: string): void {
    this.listingService.closeListing(id).subscribe(() => this.loadListings());
  }

  delete(id: string): void {
    if (confirm('Supprimer cette annonce ?')) {
      this.listingService.deleteListing(id).subscribe(() => this.loadListings());
    }
  }

  canPublish(listing: Listing): boolean {
    return listing.status === ListingStatus.DRAFT || listing.status === ListingStatus.PAUSED;
  }

  canPause(listing: Listing): boolean {
    return listing.status === ListingStatus.PUBLISHED;
  }

  canClose(listing: Listing): boolean {
    return listing.status === ListingStatus.PUBLISHED || listing.status === ListingStatus.PAUSED;
  }
}
