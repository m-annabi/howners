import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';
import { TenantRating, RATING_LABELS } from '../../../core/models/tenant-rating.model';
import { User } from '../../../core/models/user.model';
import { Rental } from '../../../core/models/rental.model';

@Component({
  selector: 'app-tenant-profile',
  templateUrl: './tenant-profile.component.html',
  styleUrls: ['./tenant-profile.component.scss']
})
export class TenantProfileComponent implements OnInit {
  loading = true;
  error: string | null = null;
  submitting = false;
  showForm = false;

  tenant: User | null = null;
  ratings: TenantRating[] = [];
  sharedRentals: Rental[] = [];
  ratedRentalIds = new Set<string>();

  form!: FormGroup;
  readonly ratingLabels = RATING_LABELS;
  readonly stars = [1, 2, 3, 4, 5];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private http: HttpClient,
    private ratingService: TenantRatingService,
    private notif: NotificationService
  ) {}

  ngOnInit(): void {
    const tenantId = this.route.snapshot.paramMap.get('id')!;

    this.form = this.fb.group({
      rentalId: [null],
      paymentRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      propertyRespectRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      communicationRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['']
    });

    forkJoin({
      profile: this.ratingService.getTenantProfile(tenantId).pipe(catchError(() => of(null))),
      ratings: this.ratingService.getRatingsForTenant(tenantId).pipe(catchError(() => of([]))),
      rentals: this.http.get<Rental[]>(`${environment.apiUrl}/rentals/by-tenant/${tenantId}`).pipe(catchError(() => of([])))
    }).subscribe(({ profile, ratings, rentals }) => {
      this.loading = false;
      if (!profile) { this.error = 'Locataire introuvable.'; return; }
      this.tenant = profile;
      this.ratings = ratings;
      this.sharedRentals = rentals;
      this.ratedRentalIds = new Set(ratings.map(r => r.rentalId).filter(Boolean) as string[]);

      if (this.sharedRentals.length > 0) {
        this.form.patchValue({ rentalId: this.sharedRentals[0].id });
      }
    });
  }

  get tenantId(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  get averageOverall(): number {
    if (!this.ratings.length) return 0;
    return this.ratings.reduce((sum, r) => sum + r.overallRating, 0) / this.ratings.length;
  }

  avgCategory(key: string): number {
    if (!this.ratings.length) return 0;
    return this.ratings.reduce((sum, r) => sum + (r as any)[key], 0) / this.ratings.length;
  }

  get canRate(): boolean {
    const rentalId = this.form.get('rentalId')?.value;
    return this.sharedRentals.length > 0 && (!rentalId || !this.ratedRentalIds.has(rentalId));
  }

  setRating(field: string, value: number): void {
    this.form.get(field)!.setValue(value);
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    const v = this.form.value;
    this.ratingService.create({
      tenantId: this.tenantId,
      rentalId: v.rentalId || undefined,
      paymentRating: v.paymentRating,
      propertyRespectRating: v.propertyRespectRating,
      communicationRating: v.communicationRating,
      comment: v.comment?.trim() || undefined
    }).subscribe({
      next: (rating) => {
        this.ratings = [rating, ...this.ratings];
        if (v.rentalId) this.ratedRentalIds.add(v.rentalId);
        this.showForm = false;
        this.submitting = false;
        this.form.reset({ rentalId: this.sharedRentals[0]?.id ?? null });
        this.notif.success('Avis enregistré !');
      },
      error: (err) => {
        this.submitting = false;
        this.notif.error(err.error?.message || 'Erreur lors de l\'envoi');
      }
    });
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  starsArray(n: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }
}
