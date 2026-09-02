import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
import { Component, OnInit } from '@angular/core';
import { ListingService } from '../../../core/services/listing.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Listing, ListingStatus, LISTING_STATUS_LABELS, LISTING_STATUS_COLORS } from '../../../core/models/listing.model';

@Component({
  selector: 'app-my-listings',
  templateUrl: './my-listings.component.html',
  styleUrls: ['./my-listings.component.scss']
})
export class MyListingsComponent implements OnInit {
  listings: Listing[] = [];
  loading = false;
  error: string | null = null;

  statusLabels = LISTING_STATUS_LABELS;
  statusColors = LISTING_STATUS_COLORS;

  constructor(
    private listingService: ListingService,
    private notificationService: NotificationService,
    private confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.loadListings();
  }

  loadListings(): void {
    this.loading = true;
    this.error = null;
    this.listingService.getMyListings().subscribe({
      next: (page) => {
        this.listings = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des annonces';
        this.loading = false;
      }
    });
  }

  publish(id: string): void {
    this.listingService.publishListing(id).subscribe({
      next: () => {
        this.notificationService.success('Annonce publiée');
        this.loadListings();
      },
      error: () => this.error = 'Erreur lors de la publication'
    });
  }

  pause(id: string): void {
    this.confirmDialog.confirm('Confirmation', 'Mettre cette annonce en pause ? Elle ne sera plus visible publiquement.', 'warning').subscribe(ok => {
      if (!ok) return;
      this.listingService.pauseListing(id).subscribe({
        next: () => {
          this.notificationService.success('Annonce mise en pause');
          this.loadListings();
        },
        error: () => this.error = 'Erreur lors de la mise en pause'
      });
    });
  }

  close(id: string): void {
    this.confirmDialog.confirm('Confirmation', 'Fermer cette annonce ? Elle n\'acceptera plus de candidatures.', 'danger').subscribe(ok => {
      if (!ok) return;
      this.listingService.closeListing(id).subscribe({
        next: () => {
          this.notificationService.success('Annonce fermée');
          this.loadListings();
        },
        error: () => this.error = 'Erreur lors de la fermeture'
      });
    });
  }

  delete(id: string): void {
    this.confirmDialog.confirm('Confirmer la suppression', 'Supprimer cette annonce ?', 'danger').subscribe(ok => {
      if (!ok) return;
      this.listingService.deleteListing(id).subscribe({
        next: () => {
          this.notificationService.success('Annonce supprimée');
          this.loadListings();
        },
        error: () => this.error = 'Erreur lors de la suppression'
      });
    });
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
