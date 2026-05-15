import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth/auth.service';

const STORAGE_KEY = 'howners.nps.dismissed';
const SHOW_AFTER_DAYS = 14;

@Component({
  selector: 'app-nps-prompt',
  templateUrl: './nps-prompt.component.html',
  styleUrls: ['./nps-prompt.component.scss']
})
export class NpsPromptComponent implements OnInit {
  visible = false;
  selectedScore: number | null = null;
  comment = '';
  submitting = false;
  scores = Array.from({ length: 11 }, (_, i) => i); // 0..10

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) return;
    if (localStorage.getItem(STORAGE_KEY)) return;

    // Don't show on the very first session — wait until they've used the product.
    // Approximation: show if the JWT was issued ≥ 14 days ago (we use the user's
    // createdAt if available). For simplicity, check backend hasAnswered.
    this.http.get<{ answered: boolean }>(`${environment.apiUrl}/feedback/nps/status`).subscribe({
      next: (r) => {
        if (r.answered) {
          // already answered — never show again
          localStorage.setItem(STORAGE_KEY, '1');
          return;
        }
        // Use the auth user's createdAt to gate display.
        this.auth.currentUser$.subscribe(user => {
          if (!user) return;
          const created = (user as any).createdAt;
          if (!created) return;
          const days = (Date.now() - new Date(created).getTime()) / 86400000;
          if (days >= SHOW_AFTER_DAYS) {
            // Delay 8s so the page settles before popping the prompt.
            setTimeout(() => this.visible = true, 8000);
          }
        });
      },
      error: () => {}
    });
  }

  select(s: number): void {
    this.selectedScore = s;
  }

  dismiss(): void {
    this.visible = false;
    localStorage.setItem(STORAGE_KEY, '1');
  }

  remindLater(): void {
    // Don't permanently dismiss — just hide for the session.
    this.visible = false;
  }

  submit(): void {
    if (this.selectedScore == null || this.submitting) return;
    this.submitting = true;
    this.http.post(`${environment.apiUrl}/feedback/nps`, {
      score: this.selectedScore,
      comment: this.comment || null
    }).subscribe({
      next: () => {
        localStorage.setItem(STORAGE_KEY, '1');
        this.visible = false;
        this.submitting = false;
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  toneClass(s: number): string {
    if (s <= 6) return 'detractor';
    if (s <= 8) return 'passive';
    return 'promoter';
  }
}
