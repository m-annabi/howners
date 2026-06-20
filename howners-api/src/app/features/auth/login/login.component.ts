import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
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

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private tenantApiService: TenantApiService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.error = null;

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
      next: (destination) => this.router.navigate([destination]),
      error: (err) => {
        this.error = err.error?.message || 'Login failed';
        this.loading = false;
      }
    });
  }
}
