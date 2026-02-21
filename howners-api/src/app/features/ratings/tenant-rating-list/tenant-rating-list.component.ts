import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantRating, RATING_CRITERIA_LABELS, RATING_CRITERIA_ICONS } from '../../../core/models/tenant-rating.model';

@Component({
  selector: 'app-tenant-rating-list',
  templateUrl: './tenant-rating-list.component.html',
  styleUrls: ['./tenant-rating-list.component.css']
})
export class TenantRatingListComponent implements OnInit {
  ratings: TenantRating[] = [];
  loading = true;
  error: string | null = null;

  criteriaLabels = RATING_CRITERIA_LABELS;
  criteriaIcons = RATING_CRITERIA_ICONS;

  constructor(
    private tenantRatingService: TenantRatingService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadRatings();
  }

  loadRatings(): void {
    this.loading = true;
    this.error = null;

    this.tenantRatingService.getMyRatings().subscribe({
      next: (ratings) => {
        this.ratings = ratings;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des évaluations';
        this.loading = false;
      }
    });
  }

  navigateToCreate(): void {
    this.router.navigate(['/ratings/new']);
  }

  editRating(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/ratings', id, 'edit']);
  }

  deleteRating(id: string, event: Event): void {
    event.stopPropagation();
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette évaluation ?')) {
      return;
    }
    this.tenantRatingService.deleteRating(id).subscribe({
      next: () => {
        this.ratings = this.ratings.filter(r => r.id !== id);
        this.notificationService.success('Évaluation supprimée avec succès');
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  getStars(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
}
