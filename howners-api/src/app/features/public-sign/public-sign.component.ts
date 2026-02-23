import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PublicContractService } from '../../core/services/public-contract.service';
import { ContractPublicView } from '../../core/models/esignature.model';

/**
 * Composant public pour la signature de contrat via token.
 * Accessible sans authentification.
 * Le locataire peut consulter le contrat et signer directement sur le PDF.
 */
@Component({
  selector: 'app-public-sign',
  templateUrl: './public-sign.component.html',
  styleUrls: ['./public-sign.component.scss']
})
export class PublicSignComponent implements OnInit {
  contract: ContractPublicView | null = null;
  loading = true;
  error: string | null = null;
  token: string | null = null;

  // Signature
  signing = false;
  signed = false;
  signatureData: string = '';
  signerName: string = '';
  acceptTerms: boolean = false;

  // PDF preview
  pdfUrl: SafeResourceUrl | null = null;
  loadingPdf = false;

  constructor(
    private route: ActivatedRoute,
    private publicContractService: PublicContractService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
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

  private loadContract(): void {
    if (!this.token) return;

    this.loading = true;
    this.error = null;

    this.publicContractService.getContractByToken(this.token).subscribe({
      next: (contract: ContractPublicView) => {
        this.contract = contract;
        this.signerName = contract.tenantName;
        this.loading = false;
        this.loadPdfPreview();
      },
      error: (err: any) => {
        this.loading = false;
        if (err.status === 400 || err.status === 404) {
          this.error = 'Le lien de signature est invalide ou a expiré. Veuillez contacter votre propriétaire.';
        } else {
          this.error = 'Une erreur est survenue lors du chargement du contrat. Veuillez réessayer plus tard.';
        }
      }
    });
  }

  private loadPdfPreview(): void {
    if (!this.token) return;

    this.loadingPdf = true;
    this.publicContractService.downloadContractPdf(this.token).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.loadingPdf = false;
      },
      error: () => {
        this.loadingPdf = false;
      }
    });
  }

  onSignatureChange(signatureData: string): void {
    this.signatureData = signatureData;
  }

  canSign(): boolean {
    return this.acceptTerms &&
           this.signerName.trim().length > 0 &&
           this.signatureData.length > 0 &&
           !this.signing;
  }

  signContract(): void {
    if (!this.token || !this.canSign()) return;

    this.signing = true;
    this.error = null;

    // Extraire la partie base64 (enlever le préfixe data:image/png;base64,)
    let signatureBase64 = this.signatureData;
    if (signatureBase64.startsWith('data:')) {
      signatureBase64 = signatureBase64.split(',')[1];
    }

    this.publicContractService.signContract(this.token, signatureBase64, this.signerName).subscribe({
      next: () => {
        this.signing = false;
        this.signed = true;
      },
      error: () => {
        this.signing = false;
        this.error = 'Impossible de signer le contrat. Veuillez réessayer.';
      }
    });
  }

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
