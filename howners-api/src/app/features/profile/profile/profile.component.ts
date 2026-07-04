import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, Subject, of } from 'rxjs';
import { takeUntil, finalize, first, filter, catchError } from 'rxjs/operators';
import { TenantService } from '../../../core/services/tenant.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { OwnerRatingService } from '../../../core/services/owner-rating.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  profileForm: FormGroup;
  user: User | null = null;
  loading = false;
  saving = false;
  isTenant = false;

  ratingsCount = 0;
  averageOverall = 0;
  readonly stars = [1, 2, 3, 4, 5];

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private tenantRatingService: TenantRatingService,
    private ownerRatingService: OwnerRatingService
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      addressLine1: [''],
      addressLine2: [''],
      postalCode: [''],
      city: [''],
      country: ['']
    });
  }

  ngOnInit(): void {
    this.loading = true;
    this.authService.currentUser$.pipe(
      filter(user => user !== null),
      first(),
      takeUntil(this.destroy$)
    ).subscribe(user => {
      this.isTenant = user?.role === 'TENANT';
      this.loadProfile();
      this.loadRatings(user!.role);
    });
  }

  loadProfile(): void {
    const profile$ = this.isTenant
      ? this.tenantService.getMyProfile()
      : this.authService.getCurrentUser();

    profile$.pipe(
      takeUntil(this.destroy$),
      finalize(() => this.loading = false)
    ).subscribe({
      next: (user) => {
        this.user = user;
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phone: user.phone || '',
          addressLine1: user.addressLine1 || '',
          addressLine2: user.addressLine2 || '',
          postalCode: user.postalCode || '',
          city: user.city || '',
          country: user.country || ''
        });
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement du profil');
      }
    });
  }

  // Note moyenne reçue, affichée dans le bandeau héro
  private loadRatings(role: string): void {
    const ratings$: Observable<Array<{ overallRating: number }>> | null = role === 'TENANT'
      ? this.tenantRatingService.getMyRatings()
      : role === 'OWNER'
        ? this.ownerRatingService.getMyRatings()
        : null;

    if (!ratings$) return;

    ratings$.pipe(
      takeUntil(this.destroy$),
      catchError(() => of([] as Array<{ overallRating: number }>))
    ).subscribe((ratings: Array<{ overallRating: number }>) => {
      this.ratingsCount = ratings.length;
      this.averageOverall = ratings.length
        ? ratings.reduce((s, r) => s + r.overallRating, 0) / ratings.length
        : 0;
    });
  }

  get initials(): string {
    if (!this.user) return '';
    return `${this.user.firstName.charAt(0)}${this.user.lastName.charAt(0)}`.toUpperCase();
  }

  // « juillet 2026 » — le pipe date sortirait le mois en anglais (locale fr non enregistrée)
  get memberSince(): string {
    if (!this.user?.createdAt) return '';
    return new Date(this.user.createdAt).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  get roleLabel(): string {
    switch (this.user?.role) {
      case 'TENANT': return 'Locataire';
      case 'ADMIN': return 'Admin';
      default: return 'Propriétaire';
    }
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      return;
    }

    this.saving = true;
    const formValue = this.profileForm.value;

    const update$ = this.isTenant
      ? this.tenantService.updateMyProfile(formValue)
      : this.authService.updateCurrentUser(formValue);

    update$.pipe(
      takeUntil(this.destroy$),
      finalize(() => this.saving = false)
    ).subscribe({
      next: (updatedUser) => {
        this.user = updatedUser;
        this.notificationService.success('Profil mis à jour avec succès');
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la mise à jour du profil');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
