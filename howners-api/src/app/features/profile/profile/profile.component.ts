import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, finalize, first, filter } from 'rxjs/operators';
import { TenantService } from '../../../core/services/tenant.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
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

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['']
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
    });
  }

  loadProfile(): void {
    if (this.isTenant) {
      this.tenantService.getMyProfile().pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      ).subscribe({
        next: (user) => {
          this.user = user;
          this.profileForm.patchValue({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            phone: user.phone || ''
          });
        },
        error: (err) => {
          console.error('Error loading profile:', err);
          this.notificationService.error('Erreur lors du chargement du profil');
        }
      });
    } else {
      // Pour les propriétaires, utiliser l'API auth
      this.authService.getCurrentUser().pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      ).subscribe({
        next: (user) => {
          this.user = user;
          this.profileForm.patchValue({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            phone: user.phone || ''
          });
        },
        error: (err) => {
          console.error('Error loading profile:', err);
          this.notificationService.error('Erreur lors du chargement du profil');
        }
      });
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
        console.error('Error updating profile:', err);
        this.notificationService.error(err.error?.message || 'Erreur lors de la mise à jour du profil');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
