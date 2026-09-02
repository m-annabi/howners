import { downloadBlob } from '../../../shared/utils/file.utils';
import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ChargeRegularisationService, Regularisation } from '../../../core/services/charge-regularisation.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentType } from '../../../core/models/document.model';

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

  /** Ajustement manuel en cours de saisie (une régularisation à la fois). */
  adjusting: Regularisation | null = null;
  adjustAmount: number | null = null;
  adjustMotif = '';
  adjustFile: File | null = null;

  constructor(
    private regulService: ChargeRegularisationService,
    private authService: AuthService,
    private notifications: NotificationService,
    private documentService: DocumentService
  ) {}

  startAdjust(regul: Regularisation): void {
    this.adjusting = regul;
    this.adjustAmount = regul.chargesReelles;
    this.adjustMotif = regul.ajustementMotif || '';
    this.adjustFile = null;
  }

  cancelAdjust(): void {
    this.adjusting = null;
  }

  onAdjustFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.adjustFile = input.files?.length ? input.files[0] : null;
  }

  get canSubmitAdjust(): boolean {
    return !!this.adjusting && this.adjustAmount != null && this.adjustAmount >= 0
      && this.adjustMotif.trim().length >= 10 && (!!this.adjustFile || !!this.adjusting.justificatifDocumentId);
  }

  /** Dépose le justificatif (document rattaché au bail) puis enregistre l'ajustement. */
  submitAdjust(): void {
    if (!this.adjusting || !this.canSubmitAdjust) return;
    const regul = this.adjusting;
    this.working = true;
    const save = (justificatifDocumentId: string) => {
      this.regulService.ajuster(regul.id, {
        chargesReelles: Number(this.adjustAmount),
        motif: this.adjustMotif.trim(),
        justificatifDocumentId
      }).subscribe({
        next: () => {
          this.working = false;
          this.adjusting = null;
          this.notifications.success('Montant ajusté, justificatif enregistré.');
          this.load();
        },
        error: (err) => {
          this.working = false;
          this.notifications.error(err.error?.message || 'Échec de l\'ajustement.');
        }
      });
    };
    if (this.adjustFile) {
      this.documentService.uploadDocument({
        file: this.adjustFile,
        documentType: DocumentType.OTHER,
        rentalId: this.rentalId,
        description: `Justificatif régularisation des charges ${regul.annee}`
      }).subscribe({
        next: (doc) => save(doc.id),
        error: () => { this.working = false; this.notifications.error('Échec du dépôt du justificatif.'); }
      });
    } else {
      save(regul.justificatifDocumentId!);
    }
  }

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
      next: (blob) => downloadBlob(blob, `decompte-charges-${regul.annee}.pdf`),
      error: () => this.notifications.error('Décompte indisponible.')
    });
  }

  get anneesDisponibles(): number[] {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2, current - 3];
  }
}
