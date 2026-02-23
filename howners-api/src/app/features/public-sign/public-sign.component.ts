import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PublicContractService } from '../../core/services/public-contract.service';
import { ContractPublicView } from '../../core/models/esignature.model';

@Component({
  selector: 'app-public-sign',
  templateUrl: './public-sign.component.html',
  styleUrls: ['./public-sign.component.scss']
})
export class PublicSignComponent implements OnInit, OnDestroy {
  contract: ContractPublicView | null = null;
  loading = true;
  error: string | null = null;
  token: string | null = null;

  signing = false;
  signed = false;
  signatureData = '';
  signerName = '';
  acceptTerms = false;

  pdfUrl: SafeResourceUrl | null = null;
  loadingPdf = false;

  private pdfObjectUrl: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private publicContractService: PublicContractService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.token = params['token'];

      if (!this.token) {
        this.error = 'Token manquant. Veuillez utiliser le lien fourni dans votre email.';
        this.loading = false;
        return;
      }

      this.loadContract();
    });
  }

  ngOnDestroy(): void {
    this.revokePdfUrl();
    this.destroy$.next();
    this.destroy$.complete();
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
        this.error = this.getErrorMessage(err);
      }
    });
  }

  private loadPdfPreview(): void {
    if (!this.token) return;

    this.loadingPdf = true;
    this.publicContractService.downloadContractPdf(this.token).subscribe({
      next: (blob: Blob) => {
        this.revokePdfUrl();
        this.pdfObjectUrl = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
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
    return this.acceptTerms
        && this.signerName.trim().length > 0
        && this.signatureData.length > 0
        && !this.signing;
  }

  signContract(): void {
    if (!this.token || !this.canSign()) return;

    this.signing = true;
    this.error = null;

    let signatureBase64 = this.signatureData;
    if (signatureBase64.startsWith('data:')) {
      signatureBase64 = signatureBase64.split(',')[1];
    }

    this.publicContractService.signContract(this.token, signatureBase64, this.signerName).subscribe({
      next: () => {
        this.signing = false;
        this.signed = true;
      },
      error: (err: any) => {
        this.signing = false;
        this.error = this.getErrorMessage(err);
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

  private revokePdfUrl(): void {
    if (this.pdfObjectUrl) {
      URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
    }
  }

  private getErrorMessage(err: any): string {
    if (err.error?.message) {
      return err.error.message;
    }
    switch (err.status) {
      case 401:
        return 'Le lien de signature est invalide. Veuillez contacter votre propriétaire.';
      case 410:
        return 'Le lien de signature a expiré. Veuillez demander un nouvel envoi à votre propriétaire.';
      case 404:
        return 'Contrat introuvable. Veuillez vérifier votre lien.';
      default:
        return 'Une erreur est survenue. Veuillez réessayer plus tard.';
    }
  }
}
