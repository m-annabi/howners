import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { Role } from '../../../core/models/user.model';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  loading = false;
  error: string | null = null;
  showPassword = false;
  referralCode: string | null = null;
  // Après inscription : on n'auto-connecte plus, on affiche l'écran « vérifiez vos e-mails ».
  submitted = false;
  successMessage = '';
  resendLoading = false;
  resendDone = false;
  roles = [
    { value: Role.OWNER, label: 'Propriétaire' },
    { value: Role.TENANT, label: 'Locataire' }
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/)]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      phone: [''],
      role: [Role.OWNER, [Validators.required]],
      referralCode: ['']
    });
  }

  ngOnInit(): void {
    const ref = this.route.snapshot.queryParamMap.get('ref');
    if (ref) {
      this.referralCode = ref.trim().toUpperCase();
      this.registerForm.patchValue({ referralCode: this.referralCode });
    }
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        // Plus d'auto-connexion : le compte est en attente de vérification par e-mail.
        this.loading = false;
        this.submitted = true;
        this.successMessage = response.message;
      },
      error: (err) => {
        this.error = err.error?.message || "L'inscription a échoué";
        this.loading = false;
      }
    });
  }

  resend(): void {
    const email = this.registerForm.get('email')?.value;
    if (!email || this.resendLoading || this.resendDone) {
      return;
    }
    this.resendLoading = true;
    this.authService.resendVerification(email).subscribe({
      // Réponse toujours générique côté serveur → on affiche le même état quoi qu'il arrive.
      next: () => { this.resendLoading = false; this.resendDone = true; },
      error: () => { this.resendLoading = false; this.resendDone = true; }
    });
  }
}
