import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  ComparaisonEdl,
  EtatDesLieuxService,
  RetenueDepot
} from '../../../core/services/etat-des-lieux.service';

@Component({
  selector: 'app-edl-comparison',
  templateUrl: './edl-comparison.component.html',
  styleUrls: ['./edl-comparison.component.scss']
})
export class EdlComparisonComponent implements OnInit {
  rentalId = '';
  comparaison: ComparaisonEdl | null = null;
  retenues: RetenueDepot[] = [];
  loading = false;
  working = false;
  error: string | null = null;
  isOwner = false;

  constructor(
    private route: ActivatedRoute,
    private edlService: EtatDesLieuxService,
    private authService: AuthService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.isOwner = this.authService.hasRole('OWNER') || this.authService.hasRole('ADMIN');
    this.rentalId = this.route.snapshot.paramMap.get('rentalId') || '';
    if (this.rentalId) this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.edlService.getComparaison(this.rentalId).subscribe({
      next: (comparaison) => {
        this.comparaison = comparaison;
        this.retenues = [...(comparaison.retenues || [])];
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Comparaison impossible.';
        this.loading = false;
      }
    });
  }

  ajouterRetenue(piece: { nom: string; etatEntree: string | null; etatSortie: string | null }): void {
    this.retenues.push({
      piece: piece.nom,
      etatEntree: piece.etatEntree,
      etatSortie: piece.etatSortie,
      motif: '',
      montant: 0
    });
  }

  supprimerRetenue(index: number): void {
    this.retenues.splice(index, 1);
  }

  get totalRetenues(): number {
    return this.retenues.reduce((sum, r) => sum + (Number(r.montant) || 0), 0);
  }

  get soldeARestituer(): number | null {
    if (this.comparaison?.depositAmount == null) return null;
    return this.comparaison.depositAmount - this.totalRetenues;
  }

  get depassementDepot(): boolean {
    return this.comparaison?.depositAmount != null
      && this.totalRetenues > this.comparaison.depositAmount;
  }

  enregistrer(): void {
    if (this.depassementDepot) return;
    this.working = true;
    this.edlService.enregistrerRetenues(this.rentalId, this.retenues).subscribe({
      next: (comparaison) => {
        this.working = false;
        this.comparaison = comparaison;
        this.retenues = [...(comparaison.retenues || [])];
        this.notifications.success('Retenues enregistrées.');
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de l\'enregistrement.');
      }
    });
  }

  valider(): void {
    if (!confirm('Valider le comparatif ? Le PDF sera généré et envoyé au locataire.')) return;
    this.working = true;
    this.edlService.validerComparaison(this.rentalId).subscribe({
      next: (comparaison) => {
        this.working = false;
        this.comparaison = comparaison;
        this.notifications.success('Comparatif validé et envoyé au locataire.');
      },
      error: (err) => {
        this.working = false;
        this.notifications.error(err.error?.message || 'Échec de la validation.');
      }
    });
  }

  telecharger(): void {
    if (!this.comparaison?.id) return;
    this.edlService.downloadComparaisonPdf(this.comparaison.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'comparatif-edl.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notifications.error('PDF indisponible.')
    });
  }
}
