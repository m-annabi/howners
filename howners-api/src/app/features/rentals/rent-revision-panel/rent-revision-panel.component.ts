import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { RentRevisionService, RevisionLoyer } from '../../../core/services/rent-revision.service';

const STATUT_LABELS: { [key: string]: string } = {
  BROUILLON: 'Brouillon',
  NOTIFIEE: 'Notifiée',
  APPLIQUEE: 'Appliquée',
  ANNULEE: 'Annulée'
};

@Component({
  selector: 'app-rent-revision-panel',
  templateUrl: './rent-revision-panel.component.html',
  styleUrls: ['./rent-revision-panel.component.scss']
})
export class RentRevisionPanelComponent implements OnInit {
  @Input() rentalId!: string;

  revisions: RevisionLoyer[] = [];
  loading = false;
  working = false;
  isOwner = false;

  statutLabels = STATUT_LABELS;

  constructor(
    private revisionService: RentRevisionService,
    private authService: AuthService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.isOwner = this.authService.hasRole('OWNER') || this.authService.hasRole('ADMIN');
    this.load();
  }

  load(): void {
    this.loading = true;
    this.revisionService.getRevisions(this.rentalId).subscribe({
      next: (revisions) => {
        this.revisions = revisions;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  calculer(): void {
    this.working = true;
    this.revisionService.calculer(this.rentalId).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success('Révision calculée selon l\'IRL.');
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Impossible de calculer la révision.');
      }
    });
  }

  notifier(revision: RevisionLoyer): void {
    this.working = true;
    this.revisionService.notifier(revision.id).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success('Courrier de révision envoyé au locataire.');
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de l\'envoi du courrier.');
      }
    });
  }

  appliquer(revision: RevisionLoyer): void {
    this.working = true;
    this.revisionService.appliquer(revision.id).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success('Nouveau loyer appliqué au bail.');
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de l\'application.');
      }
    });
  }

  annuler(revision: RevisionLoyer): void {
    this.revisionService.annuler(revision.id).subscribe({
      next: () => this.load(),
      error: (err) => this.notifications.error(err.error?.message || 'Échec de l\'annulation.')
    });
  }

  telechargerCourrier(revision: RevisionLoyer): void {
    this.revisionService.downloadCourrier(revision.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'revision-loyer.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notifications.error('Courrier indisponible.')
    });
  }
}
