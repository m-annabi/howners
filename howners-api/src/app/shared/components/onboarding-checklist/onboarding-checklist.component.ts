import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { OnboardingService, OnboardingStatus } from '../../../core/services/onboarding.service';

const DISMISS_KEY = 'hw_onboarding_dismissed';

@Component({
  selector: 'app-onboarding-checklist',
  templateUrl: './onboarding-checklist.component.html',
  styleUrls: ['./onboarding-checklist.component.scss']
})
export class OnboardingChecklistComponent implements OnInit {

  status: OnboardingStatus | null = null;
  loading = true;
  dismissed = false;

  /** Emis lorsque le composant est masque (dismissed ou complete). */
  @Output() hidden = new EventEmitter<void>();

  constructor(
    private onboardingService: OnboardingService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.dismissed = localStorage.getItem(DISMISS_KEY) === 'true';
    if (this.dismissed) {
      this.loading = false;
      this.hidden.emit();
      return;
    }
    this.onboardingService.getStatus().subscribe({
      next: status => {
        this.status = status;
        this.loading = false;
        if (status.completed) {
          this.hidden.emit();
        }
      },
      error: () => {
        this.loading = false;
        this.hidden.emit();
      }
    });
  }

  get visible(): boolean {
    return !this.loading && !this.dismissed && !!this.status && !this.status.completed;
  }

  dismiss(): void {
    localStorage.setItem(DISMISS_KEY, 'true');
    this.dismissed = true;
    this.hidden.emit();
  }

  navigateTo(link: string): void {
    this.router.navigate([link]);
  }
}
