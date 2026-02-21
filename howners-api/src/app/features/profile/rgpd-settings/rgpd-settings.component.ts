import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RgpdService } from '../../../core/services/rgpd.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  ConsentType,
  ConsentResponse,
  CONSENT_TYPE_LABELS
} from '../../../core/models/rgpd.model';

@Component({
  selector: 'app-rgpd-settings',
  templateUrl: './rgpd-settings.component.html',
  styles: [`
    .consent-toggle { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #eee; }
    .danger-zone { border: 2px solid #dc3545; border-radius: 8px; padding: 20px; margin-top: 20px; }
  `]
})
export class RgpdSettingsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  consents: ConsentResponse[] = [];
  loading = false;
  exporting = false;
  exportingPdf = false;
  deleting = false;

  ConsentType = ConsentType;
  consentLabels = CONSENT_TYPE_LABELS;

  allConsentTypes = Object.values(ConsentType);

  constructor(
    private rgpdService: RgpdService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadConsents();
  }

  loadConsents(): void {
    this.loading = true;
    this.rgpdService.getConsents().pipe(takeUntil(this.destroy$)).subscribe({
      next: (consents) => {
        this.consents = consents;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  isConsentGranted(type: ConsentType): boolean {
    const consent = this.consents.find(c => c.consentType === type);
    return consent?.granted || false;
  }

  toggleConsent(type: ConsentType): void {
    const currentlyGranted = this.isConsentGranted(type);
    this.rgpdService.recordConsent({ consentType: type, granted: !currentlyGranted })
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.loadConsents();
          this.notificationService.success(currentlyGranted ? 'Consentement retiré' : 'Consentement accordé');
        },
        error: () => this.notificationService.error('Erreur lors de la mise à jour du consentement')
      });
  }

  exportJson(): void {
    this.exporting = true;
    this.rgpdService.exportData().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'export-rgpd.json';
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
        this.notificationService.success('Export JSON téléchargé');
      },
      error: () => {
        this.exporting = false;
        this.notificationService.error('Erreur lors de l\'export');
      }
    });
  }

  exportPdf(): void {
    this.exportingPdf = true;
    this.rgpdService.exportDataAsPdf().pipe(takeUntil(this.destroy$)).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'export-rgpd.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportingPdf = false;
        this.notificationService.success('Export PDF téléchargé');
      },
      error: () => {
        this.exportingPdf = false;
        this.notificationService.error('Erreur lors de l\'export PDF');
      }
    });
  }

  requestErasure(): void {
    const confirmed = confirm(
      'ATTENTION : Cette action est IRRÉVERSIBLE.\n\n' +
      'Toutes vos données personnelles seront anonymisées et vos fichiers supprimés.\n' +
      'Votre compte sera désactivé.\n\n' +
      'Êtes-vous sûr de vouloir continuer ?'
    );

    if (!confirmed) return;

    const doubleConfirm = confirm('Dernière confirmation : supprimer définitivement votre compte ?');
    if (!doubleConfirm) return;

    this.deleting = true;
    this.rgpdService.requestErasure().pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.deleting = false;
        this.notificationService.success('Votre compte a été anonymisé. Vous allez être déconnecté.');
        // Redirect to login after a delay
        setTimeout(() => window.location.href = '/auth/login', 2000);
      },
      error: (err) => {
        this.deleting = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
