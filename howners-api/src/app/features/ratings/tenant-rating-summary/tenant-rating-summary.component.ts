import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { TenantRatingSummary, TenantRating, RATING_CRITERIA_LABELS, RATING_CRITERIA_ICONS } from '../../../core/models/tenant-rating.model';

@Component({
  selector: 'app-tenant-rating-summary',
  templateUrl: './tenant-rating-summary.component.html',
  styleUrls: ['./tenant-rating-summary.component.css']
})
export class TenantRatingSummaryComponent implements OnChanges {
  @Input() tenantId!: string;

  summary: TenantRatingSummary | null = null;
  recentRatings: TenantRating[] = [];
  loading = true;
  error: string | null = null;

  criteriaLabels = RATING_CRITERIA_LABELS;
  criteriaIcons = RATING_CRITERIA_ICONS;

  constructor(private tenantRatingService: TenantRatingService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tenantId'] && this.tenantId) {
      this.loadData();
    }
  }

  loadData(): void {
    this.loading = true;
    this.error = null;

    this.tenantRatingService.getTenantSummary(this.tenantId).subscribe({
      next: (summary) => {
        this.summary = summary;
        // Also load recent ratings
        this.tenantRatingService.getRatingsByTenant(this.tenantId).subscribe({
          next: (ratings) => {
            this.recentRatings = ratings.slice(0, 3);
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement';
        this.loading = false;
      }
    });
  }

  getStars(): number[] {
    return [1, 2, 3, 4, 5];
  }

  isStarFilled(star: number, rating: number | null): boolean {
    return rating != null && star <= Math.round(rating);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
}
