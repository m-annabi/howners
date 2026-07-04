import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { OwnerRatingService } from '../../../core/services/owner-rating.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';
import { OwnerRating, OWNER_RATING_LABELS } from '../../../core/models/owner-rating.model';
import { User } from '../../../core/models/user.model';
import { Rental } from '../../../core/models/rental.model';

@Component({
  selector: 'app-owner-profile',
  templateUrl: './owner-profile.component.html',
  styleUrls: ['./owner-profile.component.scss']
})
export class OwnerProfileComponent implements OnInit {
  loading = true;
  error: string | null = null;
  submitting = false;
  showForm = false;

  owner: User | null = null;
  ratings: OwnerRating[] = [];
  sharedRentals: Rental[] = [];
  ratedRentalIds = new Set<string>();

  form!: FormGroup;
  readonly ratingLabels = OWNER_RATING_LABELS;
  readonly stars = [1, 2, 3, 4, 5];

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private http: HttpClient,
    private ratingService: OwnerRatingService,
    private notif: NotificationService
  ) {}

  ngOnInit(): void {
    const ownerId = this.route.snapshot.paramMap.get('id')!;

    this.form = this.fb.group({
      rentalId: [null],
      communicationRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      responsivenessRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      contractRespectRating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['']
    });

    forkJoin({
      profile: this.ratingService.getOwnerProfile(ownerId).pipe(catchError(() => of(null))),
      ratings: this.ratingService.getRatingsForOwner(ownerId).pipe(catchError(() => of([]))),
      rentals: this.http.get<Rental[]>(`${environment.apiUrl}/rentals/by-owner/${ownerId}`).pipe(catchError(() => of([])))
    }).subscribe(({ profile, ratings, rentals }) => {
      this.loading = false;
      if (!profile) { this.error = 'Propriétaire introuvable.'; return; }
      this.owner = profile;
      this.ratings = ratings;
      this.sharedRentals = rentals;
      this.ratedRentalIds = new Set(ratings.map(r => r.rentalId).filter(Boolean) as string[]);

      if (this.sharedRentals.length > 0) {
        this.form.patchValue({ rentalId: this.sharedRentals[0].id });
      }
    });
  }

  get ownerId(): string {
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
      ownerId: this.ownerId,
      rentalId: v.rentalId || undefined,
      communicationRating: v.communicationRating,
      responsivenessRating: v.responsivenessRating,
      contractRespectRating: v.contractRespectRating,
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
}
