import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ChargeRegularisationService, Regularisation } from '../../../core/services/charge-regularisation.service';

const STATUT_LABELS: { [key: string]: string } = {
  BROUILLON: 'Brouillon',
  ENVOYEE: 'Envoyée',
  SOLDEE: 'Soldée'
};

@Component({
  selector: 'app-charge-regularisation-panel',
  templateUrl: './charge-regularisation-panel.component.html',
  styleUrls: ['./charge-regularisation-panel.component.scss']
})
export class ChargeRegularisationPanelComponent implements OnInit {
  @Input() rentalId!: string;

  regularisations: Regularisation[] = [];
  loading = false;
  working = false;
  isOwner = false;
  annee = new Date().getFullYear() - 1;

  statutLabels = STATUT_LABELS;

  constructor(
    private regulService: ChargeRegularisationService,
    private authService: AuthService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.isOwner = this.authService.hasRole('OWNER') || this.authService.hasRole('ADMIN');
    this.load();
  }

  load(): void {
    this.loading = true;
    this.regulService.getRegularisations(this.rentalId).subscribe({
      next: (regularisations) => {
        this.regularisations = regularisations;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  calculer(): void {
    this.working = true;
    this.regulService.calculer(this.rentalId, this.annee).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success(`Régularisation ${this.annee} calculée.`);
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Impossible de calculer la régularisation.');
      }
    });
  }

  envoyer(regul: Regularisation): void {
    this.working = true;
    this.regulService.envoyer(regul.id).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success('Décompte envoyé au locataire.');
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de l\'envoi du décompte.');
      }
    });
  }

  creerPaiement(regul: Regularisation): void {
    this.working = true;
    this.regulService.creerPaiement(regul.id).subscribe({
      next: () => {
        this.working = false;
        this.notifications.success('Paiement complémentaire créé (échéance +30 jours).');
        this.load();
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de la création du paiement.');
      }
    });
  }

  telecharger(regul: Regularisation): void {
    this.regulService.downloadDecompte(regul.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `decompte-charges-${regul.annee}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notifications.error('Décompte indisponible.')
    });
  }

  get anneesDisponibles(): number[] {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2, current - 3];
  }
}
