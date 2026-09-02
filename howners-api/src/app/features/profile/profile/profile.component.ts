import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject, of } from 'rxjs';
import { takeUntil, finalize, first, filter, catchError } from 'rxjs/operators';
import { TenantService } from '../../../core/services/tenant.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { OwnerRatingService } from '../../../core/services/owner-rating.service';
import { StripeConnectService, StripeConnectStatus } from '../../../core/services/stripe-connect.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  profileForm: FormGroup;
  paymentSettingsForm: FormGroup;
  user: User | null = null;
  loading = false;
  saving = false;
  isTenant = false;

  ratingsCount = 0;
  averageOverall = 0;
  readonly stars = [1, 2, 3, 4, 5];

  // Réglages de paiement (bailleur uniquement) : coordonnées déclaratives + activation carte en ligne.
  connectStatus: StripeConnectStatus | null = null;
  loadingConnect = false;
  startingOnboarding = false;
  savingPaymentSettings = false;

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private tenantRatingService: TenantRatingService,
    private ownerRatingService: OwnerRatingService,
    private stripeConnectService: StripeConnectService,
    private route: ActivatedRoute,
    private router: Router
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

    this.paymentSettingsForm = this.fb.group({
      paymentInstructions: ['', Validators.maxLength(2000)],
      acceptOnlinePayments: [false],
      // null = quittance envoyée dès la confirmation du paiement ; sinon le jour du mois choisi
      receiptSendDay: [null as number | null]
    });
  }

  get isOwner(): boolean {
    return this.user?.role === 'OWNER';
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
      if (user?.role === 'OWNER') {
        this.handleStripeConnectReturn();
        this.loadConnectStatus();
      }
    });
  }

  /** Retour d'onboarding Stripe Connect (?stripe-connect=return|refresh|not-configured). */
  private handleStripeConnectReturn(): void {
    const status = this.route.snapshot.queryParamMap.get('stripe-connect');
    if (!status) return;

    if (status === 'return') {
      this.notificationService.success('Compte Stripe mis à jour. Vérification du statut…');
    } else if (status === 'refresh') {
      this.notificationService.info('Onboarding Stripe interrompu — vous pouvez le reprendre.');
    } else if (status === 'not-configured') {
      this.notificationService.error('Le paiement en ligne n\'est pas encore configuré sur la plateforme.');
    }
    // Nettoie l'URL pour ne pas re-déclencher le message au prochain rafraîchissement.
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  loadConnectStatus(): void {
    this.loadingConnect = true;
    this.stripeConnectService.getStatus().pipe(
      takeUntil(this.destroy$),
      finalize(() => this.loadingConnect = false)
    ).subscribe({
      next: (status) => {
        this.connectStatus = status;
        this.paymentSettingsForm.patchValue({
          paymentInstructions: status.paymentInstructions || '',
          acceptOnlinePayments: status.acceptOnlinePayments,
          receiptSendDay: status.receiptSendDay ?? null
        });
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement du statut de paiement en ligne');
      }
    });
  }

  /** Jours du mois proposés pour l'envoi différé des quittances (1 à 28, valable tous les mois). */
  readonly receiptDays = Array.from({ length: 28 }, (_, i) => i + 1);

  /** Frais Stripe indicatifs (cartes européennes) : 1,5 % + 0,25 € par transaction, à la charge du bailleur. */
  readonly stripeFeePercent = 1.5;
  readonly stripeFeeFixed = 0.25;

  /** Exemple chiffré des frais sur un loyer de 800 € (commission plateforme + frais Stripe). */
  get feeExample(): { platform: number; stripe: number; total: number } | null {
    const pct = this.connectStatus?.platformFeePercent;
    if (pct == null) return null;
    const rent = 800;
    const platform = Math.round(rent * pct) / 100;
    const stripe = Math.round((rent * this.stripeFeePercent / 100 + this.stripeFeeFixed) * 100) / 100;
    return { platform, stripe, total: Math.round((platform + stripe) * 100) / 100 };
  }

  get connectBadgeClass(): string {
    switch (this.connectStatus?.status) {
      case 'COMPLETED': return 'hw-badge--success';
      case 'PENDING': return 'hw-badge--warning';
      default: return 'hw-badge--secondary';
    }
  }

  get connectStatusLabel(): string {
    switch (this.connectStatus?.status) {
      case 'COMPLETED': return 'Compte vérifié';
      case 'PENDING': return 'Vérification en cours';
      default: return 'Non connecté';
    }
  }

  startOnboarding(): void {
    this.startingOnboarding = true;
    this.stripeConnectService.startOnboarding().pipe(
      takeUntil(this.destroy$),
      finalize(() => this.startingOnboarding = false)
    ).subscribe({
      next: (status) => {
        if (status.onboardingUrl) {
          window.location.href = status.onboardingUrl;
        } else {
          this.notificationService.error('Impossible de démarrer l\'onboarding Stripe');
        }
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors du démarrage de l\'onboarding Stripe');
      }
    });
  }

  onSubmitPaymentSettings(): void {
    if (this.paymentSettingsForm.invalid) return;

    this.savingPaymentSettings = true;
    const v = this.paymentSettingsForm.value;
    const payload = { ...v, receiptSendDay: v.receiptSendDay ? Number(v.receiptSendDay) : null };
    this.stripeConnectService.updatePaymentSettings(payload).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.savingPaymentSettings = false)
    ).subscribe({
      next: (status) => {
        this.connectStatus = status;
        this.notificationService.success('Réglages de paiement enregistrés');
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de l\'enregistrement des réglages de paiement');
      }
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
