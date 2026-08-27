import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit {
  state: 'loading' | 'success' | 'error' = 'loading';
  message = '';

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state = 'error';
      this.message = 'Lien de vérification invalide.';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (res) => {
        this.state = 'success';
        this.message = res.message;
      },
      error: (err) => {
        this.state = 'error';
        this.message = err.error?.message || 'Lien invalide ou expiré.';
      }
    });
  }
}
