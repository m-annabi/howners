import { Component, OnInit } from '@angular/core';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { TenantRating } from '../../../core/models/tenant-rating.model';

const PAGE_SIZE = 5;

@Component({
  selector: 'app-tenant-avis',
  templateUrl: './tenant-avis.component.html',
  styleUrls: ['./tenant-avis.component.scss']
})
export class TenantAvisComponent implements OnInit {
  loading = true;
  ratings: TenantRating[] = [];
  visibleCount = PAGE_SIZE;
  readonly stars = [1, 2, 3, 4, 5];

  constructor(private ratingService: TenantRatingService) {}

  ngOnInit(): void {
    this.ratingService.getMyRatings().subscribe({
      next: (r) => { this.ratings = r; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get visibleRatings(): TenantRating[] {
    return this.ratings.slice(0, this.visibleCount);
  }

  get hasMore(): boolean {
    return this.visibleCount < this.ratings.length;
  }

  get remainingCount(): number {
    return this.ratings.length - this.visibleCount;
  }

  showMore(): void {
    this.visibleCount += PAGE_SIZE;
  }

  get averageOverall(): number {
    if (!this.ratings.length) return 0;
    return this.ratings.reduce((s, r) => s + r.overallRating, 0) / this.ratings.length;
  }

  avgCategory(key: string): number {
    if (!this.ratings.length) return 0;
    return this.ratings.reduce((s, r) => s + (r as any)[key], 0) / this.ratings.length;
  }

  // Distribution des notes 1→5 en pourcentage (pour l'histogramme)
  distribution(star: number): number {
    if (!this.ratings.length) return 0;
    const count = this.ratings.filter(r => Math.round(r.overallRating) === star).length;
    return (count / this.ratings.length) * 100;
  }

  asAny(r: TenantRating): any { return r; }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
