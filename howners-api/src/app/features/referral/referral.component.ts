import { Component, OnInit } from '@angular/core';
import { ReferralService, ReferralSummary } from '../../core/services/referral.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-referral',
  templateUrl: './referral.component.html',
  styleUrls: ['./referral.component.scss']
})
export class ReferralComponent implements OnInit {
  summary: ReferralSummary | null = null;
  loading = true;
  copied = false;

  constructor(
    private referralService: ReferralService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.referralService.getMySummary().subscribe({
      next: (s) => { this.summary = s; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  async copyLink(): Promise<void> {
    if (!this.summary) return;
    try {
      await navigator.clipboard.writeText(this.summary.shareUrl);
      this.copied = true;
      setTimeout(() => this.copied = false, 2500);
    } catch {
      this.notifications.error('Impossible de copier le lien.');
    }
  }

  async share(): Promise<void> {
    if (!this.summary) return;
    const nav = navigator as any;
    if (nav.share) {
      try {
        await nav.share({
          title: 'Découvrez Howners',
          text: 'Je gère mes biens locatifs avec Howners. Inscris-toi avec mon lien :',
          url: this.summary.shareUrl
        });
      } catch { /* user cancelled */ }
    } else {
      this.copyLink();
    }
  }
}
