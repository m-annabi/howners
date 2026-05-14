import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PublicContractService } from '../../core/services/public-contract.service';
import { ContractPublicView, SigningRedirectResponse } from '../../core/models/esignature.model';
import { SignaturePadComponent } from '../../shared/components/signature-pad/signature-pad.component';

/**
 * Composant public pour la signature de contrat via token
 * Accessible sans authentification
 */
@Component({
  selector: 'app-public-sign',
  templateUrl: './public-sign.component.html',
  styleUrls: ['./public-sign.component.scss']
})
export class PublicSignComponent implements OnInit {
  @ViewChild(SignaturePadComponent) signaturePad?: SignaturePadComponent;

  contract: ContractPublicView | null = null;
  loading = true;
  error: string | null = null;
  token: string | null = null;
  signing = false;
  signed = false;
  signatureData: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private publicContractService: PublicContractService
  ) {}

  ngOnInit(): void {
    // Récupérer le token depuis les query params
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];

      if (!this.token) {
        this.error = 'Token manquant. Veuillez utiliser le lien fourni dans votre email.';
        this.loading = false;
        return;
      }

      this.loadContract();
    });
  }

  /**
   * Charge le contrat via le token
   */
  private loadContract(): void {
    if (!this.token) return;

    this.loading = true;
    this.error = null;

    this.publicContractService.getContractByToken(this.token).subscribe({
      next: (contract: ContractPublicView) => {
        this.contract = contract;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement du contrat:', err);
        this.loading = false;

        if (err.status === 400 || err.status === 404) {
          this.error = 'Le lien de signature est invalide ou a expiré. Veuillez contacter votre propriétaire.';
        } else {
          this.error = 'Une erreur est survenue lors du chargement du contrat. Veuillez réessayer plus tard.';
        }
      }
    });
  }

  /**
   * Initie le processus de signature
   */
  signContract(): void {
    if (!this.token || this.signing) return;

    this.signing = true;
    this.error = null;

    // URL de retour après signature
    const returnUrl = `${window.location.origin}/contracts/sign/complete?token=${this.token}`;

    this.publicContractService.getSigningRedirect(this.token, returnUrl).subscribe({
      next: (response: SigningRedirectResponse) => {
        // Rediriger vers DocuSign
        window.location.href = response.signingUrl;
      },
      error: (err: any) => {
        console.error('Erreur lors de l\'obtention de l\'URL de signature:', err);
        this.signing = false;
        this.error = 'Impossible d\'ouvrir la page de signature. Veuillez réessayer.';
      }
    });
  }

  /**
   * Signe le contrat via le canvas HTML5.
   */
  signWithCanvas(): void {
    if (!this.token || this.signing) return;
    if (!this.signatureData) {
      this.error = 'Veuillez apposer votre signature avant de valider.';
      return;
    }

    this.signing = true;
    this.error = null;

    this.publicContractService.signWithCanvas(this.token, this.signatureData).subscribe({
      next: () => {
        this.signing = false;
        this.signed = true;
      },
      error: (err: any) => {
        console.error('Erreur lors de la signature canvas:', err);
        this.signing = false;
        this.error = err?.error?.message
          || 'Impossible de finaliser la signature. Veuillez réessayer.';
      }
    });
  }

  onSignatureChange(dataUrl: string): void {
    this.signatureData = dataUrl;
    if (this.error && dataUrl) {
      this.error = null;
    }
  }

  clearSignature(): void {
    this.signaturePad?.clear();
    this.signatureData = '';
  }

  /**
   * Formate une date au format français
   */
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }
}
