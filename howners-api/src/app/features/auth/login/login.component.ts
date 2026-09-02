import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { switchMap, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { TenantApiService } from '../../../core/services/tenant-api.service';
import { RentalStatus } from '../../../core/models/rental.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  error: string | null = null;
  showPassword = false;
  // Connexion refusée faute de vérification d'e-mail → on propose de renvoyer le lien.
  unverified = false;
  resendLoading = false;
  resendDone = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private tenantApiService: TenantApiService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  /**
   * Destination demandée avant la redirection vers le login (posée par AuthGuard).
   * Seul un chemin interne est accepté — jamais une URL absolue (open redirect).
   */
  private safeReturnUrl(): string | null {
    const url = this.route.snapshot.queryParamMap.get('returnUrl');
    return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/auth/') ? url : null;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.error = null;
    this.unverified = false;
    this.resendDone = false;

    this.authService.login(this.loginForm.value).pipe(
      switchMap(response => {
        if (response.user?.role !== 'TENANT') {
          return of('/dashboard');
        }
        // Pour un locataire : vérifie s'il a une location active pour choisir où atterrir
        return this.tenantApiService.getMyRentals().pipe(
          map(rentals => {
            const hasActive = rentals.some(r =>
              r.status === RentalStatus.ACTIVE || r.status === RentalStatus.EXITING
            );
            return hasActive ? '/tenant/dashboard' : '/listings';
          }),
          catchError(() => of('/tenant/dashboard'))
        );
      })
    ).subscribe({
      next: (destination) => this.router.navigateByUrl(this.safeReturnUrl() ?? destination),
      error: (err) => {
        this.error = err.error?.message || 'La connexion a échoué';
        this.unverified = /v[ée]rifi/i.test(this.error || '');
        this.loading = false;
      }
    });
  }

  resendVerification(): void {
    const email = this.loginForm.get('email')?.value;
    if (!email || this.resendLoading || this.resendDone) {
      return;
    }
    this.resendLoading = true;
    this.authService.resendVerification(email).subscribe({
      next: () => { this.resendLoading = false; this.resendDone = true; },
      error: () => { this.resendLoading = false; this.resendDone = true; }
    });
  }
}
