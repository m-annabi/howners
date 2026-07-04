import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, first } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { TenantRatingService } from '../../core/services/tenant-rating.service';
import { OwnerRatingService } from '../../core/services/owner-rating.service';
import { TenantRating } from '../../core/models/tenant-rating.model';
import { OwnerRating } from '../../core/models/owner-rating.model';

const PAGE_SIZE = 5;

type AnyRating = TenantRating | OwnerRating;

interface RatingCategory {
  key: string;
  label: string;
  shortLabel: string;
  icon: string;
}

const TENANT_CATEGORIES: RatingCategory[] = [
  { key: 'paymentRating',         label: 'Paiement du loyer', shortLabel: 'Paiement', icon: 'bi-credit-card' },
  { key: 'propertyRespectRating', label: 'Respect du bien',   shortLabel: 'Bien',     icon: 'bi-house-check' },
  { key: 'communicationRating',   label: 'Communication',     shortLabel: 'Comm.',    icon: 'bi-chat-dots' }
];

const OWNER_CATEGORIES: RatingCategory[] = [
  { key: 'communicationRating',   label: 'Communication',      shortLabel: 'Comm.',     icon: 'bi-chat-dots' },
  { key: 'responsivenessRating',  label: 'Réactivité',         shortLabel: 'Réactivité', icon: 'bi-tools' },
  { key: 'contractRespectRating', label: 'Respect du contrat', shortLabel: 'Contrat',   icon: 'bi-file-earmark-check' }
];

@Component({
  selector: 'app-my-avis',
  templateUrl: './my-avis.component.html',
  styleUrls: ['./my-avis.component.scss']
})
export class MyAvisComponent implements OnInit {
  loading = true;
  error: string | null = null;
  ratings: AnyRating[] = [];
  visibleCount = PAGE_SIZE;
  readonly stars = [1, 2, 3, 4, 5];

  isOwner = false;
  categories: RatingCategory[] = TENANT_CATEGORIES;
  subtitle = '';
  emptyText = '';

  constructor(
    private authService: AuthService,
    private tenantRatingService: TenantRatingService,
    private ownerRatingService: OwnerRatingService
  ) {}

  ngOnInit(): void {
    // L'utilisateur courant arrive en asynchrone au rechargement de la page
    this.authService.currentUser$.pipe(
      filter(user => user !== null),
      first()
    ).subscribe(user => this.init(user!.role));
  }

  private init(role: string): void {
    this.isOwner = role === 'OWNER' || role === 'ADMIN';
    this.categories = this.isOwner ? OWNER_CATEGORIES : TENANT_CATEGORIES;
    this.subtitle = this.isOwner
      ? 'Les évaluations laissées par vos locataires'
      : 'Les évaluations laissées par vos propriétaires';
    this.emptyText = this.isOwner
      ? 'Les locataires qui ont loué chez vous peuvent vous laisser une évaluation à la fin de leur location.'
      : 'Les propriétaires chez qui vous avez loué peuvent vous laisser une évaluation à la fin de votre location.';

    const ratings$: Observable<AnyRating[]> = this.isOwner
      ? this.ownerRatingService.getMyRatings()
      : this.tenantRatingService.getMyRatings();

    ratings$.subscribe({
      next: (r) => { this.ratings = r; this.loading = false; },
      error: () => { this.error = 'Impossible de charger vos avis pour le moment.'; this.loading = false; }
    });
  }

  get visibleRatings(): AnyRating[] {
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

  asAny(r: AnyRating): any { return r; }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
