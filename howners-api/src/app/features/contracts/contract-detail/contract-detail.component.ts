import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { ContractService } from '../../../core/services/contract.service';
import { SignatureService } from '../../../core/services/signature.service';
import { EsignatureService } from '../../../core/services/esignature.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Contract,
  ContractVersion,
  ContractStatus,
  CONTRACT_STATUS_LABELS,
  CONTRACT_STATUS_COLORS,
  UpdateContractRequest
} from '../../../core/models/contract.model';
import {
  Signature,
  CreateSignatureRequest,
  SIGNATURE_STATUS_LABELS
} from '../../../core/models/signature.model';
import {
  SignatureRequestResponse,
  SignatureRequestStatus,
  SignatureStatusHelper
} from '../../../core/models/esignature.model';

@Component({
  selector: 'app-contract-detail',
  templateUrl: './contract-detail.component.html',
  styleUrls: ['./contract-detail.component.css']
})
export class ContractDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  contract: Contract | null = null;
  versions: ContractVersion[] = [];
  signatures: Signature[] = [];
  loading = false;
  error: string | null = null;
  showVersions = false;
  showSignaturePad = false;
  signing = false;
  currentUserId: string | null = null;

  // Signature Dialog
  showSignDialog = false;
  contractContent = '';

  // E-Signature (DocuSign - deprecated)
  signatureRequest: SignatureRequestResponse | null = null;
  loadingSignatureRequest = false;
  sendingForSignature = false;
  resendingEmail = false;
  cancellingSignature = false;

  // Enums et constantes pour le template
  ContractStatus = ContractStatus;
  SignatureRequestStatus = SignatureRequestStatus;
  SignatureStatusHelper = SignatureStatusHelper;
  contractStatusLabels = CONTRACT_STATUS_LABELS;
  contractStatusColors = CONTRACT_STATUS_COLORS;
  signatureStatusLabels = SIGNATURE_STATUS_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private signatureService: SignatureService,
    private esignatureService: EsignatureService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {
    this.authService.currentUser$.subscribe(user => {
      this.currentUserId = user?.id || null;
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadContract(id);
      this.loadSignatures(id);
      this.loadSignatureRequest(id);
    }
  }

  loadContract(id: string): void {
    this.loading = true;
    this.error = null;

    this.contractService.getContract(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading contract:', err);
        this.error = 'Erreur lors du chargement du contrat';
        this.loading = false;
      }
    });
  }

  loadVersions(): void {
    if (!this.contract) return;

    this.contractService.getContractVersions(this.contract.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (versions) => {
        this.versions = versions;
        this.showVersions = true;
      },
      error: (err) => {
        console.error('Error loading versions:', err);
        this.notificationService.error('Erreur lors du chargement des versions');
      }
    });
  }

  getStatusColor(status: ContractStatus): string {
    return this.contractStatusColors[status];
  }

  getStatusLabel(status: ContractStatus): string {
    return this.contractStatusLabels[status];
  }

  sendContract(): void {
    if (!this.contract) return;

    if (confirm('Êtes-vous sûr de vouloir envoyer ce contrat au locataire ?')) {
      const request: UpdateContractRequest = {
        status: ContractStatus.SENT
      };

      this.contractService.updateContract(this.contract.id, request).pipe(takeUntil(this.destroy$)).subscribe({
        next: (updatedContract) => {
          this.contract = updatedContract;
          this.notificationService.success('Contrat envoyé avec succès');
        },
        error: (err) => {
          console.error('Error sending contract:', err);
          alert('Erreur lors de l\'envoi du contrat');
        }
      });
    }
  }

  downloadPdf(): void {
    if (!this.contract) {
      this.notificationService.warning('Aucun document disponible');
      return;
    }

    this.contractService.downloadPdf(this.contract.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrat-${this.contract!.contractNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading PDF:', err);
        this.notificationService.error('Erreur lors du téléchargement du PDF');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/contracts']);
  }

  deleteContract(): void {
    if (!this.contract) return;

    if (this.contract.status !== ContractStatus.DRAFT) {
      this.notificationService.warning('Seuls les contrats en brouillon peuvent être supprimés');
      return;
    }

    if (confirm(`Êtes-vous sûr de vouloir supprimer le contrat ${this.contract.contractNumber} ?`)) {
      this.contractService.deleteContract(this.contract.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.notificationService.success('Contrat supprimé avec succès');
          this.goBack();
        },
        error: (err) => {
          console.error('Error deleting contract:', err);
          this.notificationService.error('Erreur lors de la suppression du contrat');
        }
      });
    }
  }

  loadSignatures(contractId: string): void {
    this.signatureService.getContractSignatures(contractId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (signatures) => {
        this.signatures = signatures;
      },
      error: (err) => {
        console.error('Error loading signatures:', err);
      }
    });
  }

  toggleSignaturePad(): void {
    this.showSignaturePad = !this.showSignaturePad;
  }

  onSignatureComplete(signatureData: string): void {
    if (!this.contract) return;

    this.signing = true;

    // Extraire la partie Base64 de la signature (enlever le préfixe data:image/png;base64,)
    let signatureBase64 = signatureData;
    if (signatureBase64.startsWith('data:')) {
      signatureBase64 = signatureBase64.split(',')[1];
    }

    const userAgent = navigator.userAgent;

    const request: CreateSignatureRequest = {
      contractId: this.contract.id,
      signatureData: signatureBase64,
      ipAddress: 'frontend', // L'IP sera ajoutée côté backend si nécessaire
      userAgent: userAgent
    };

    console.log('Creating signature with request:', {
      contractId: request.contractId,
      dataLength: signatureBase64.length,
      userAgent: userAgent
    });

    this.signatureService.createSignature(request).pipe(takeUntil(this.destroy$)).subscribe({
      next: (signature) => {
        console.log('Signature created successfully:', signature);
        this.signing = false;
        this.showSignaturePad = false;
        this.notificationService.success('Contrat signé avec succès!');

        // Recharger le contrat et les signatures
        this.loadContract(this.contract!.id);
        this.loadSignatures(this.contract!.id);
      },
      error: (err) => {
        console.error('Error creating signature:', err);
        console.error('Error details:', err.error);
        this.signing = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la signature du contrat');
      }
    });
  }

  onSignatureCleared(): void {
    console.log('Signature cleared');
  }

  canSign(): boolean {
    if (!this.contract || !this.currentUserId) return false;

    // Le contrat doit être en statut SENT
    if (this.contract.status !== ContractStatus.SENT) return false;

    // L'utilisateur ne doit pas avoir déjà signé
    const alreadySigned = this.signatures.some(s => s.signerId === this.currentUserId);
    return !alreadySigned;
  }

  hasUserSigned(): boolean {
    if (!this.currentUserId) return false;
    return this.signatures.some(s => s.signerId === this.currentUserId);
  }

  // === New Signature Dialog Methods ===

  /**
   * Ouvre le dialogue de signature avec le contenu du contrat
   */
  openSignDialog(): void {
    if (!this.contract) return;

    // Préparer le contenu du contrat pour affichage
    this.contractContent = this.prepareContractContent();
    this.showSignDialog = true;
  }

  /**
   * Prépare le contenu du contrat pour affichage dans le dialogue
   */
  prepareContractContent(): string {
    if (!this.contract) return '';

    return `
      <h4>${this.contract.contractNumber}</h4>
      <p><strong>Propriété:</strong> ${this.contract.rentalPropertyName}</p>
      <p><strong>Locataire:</strong> ${this.contract.tenantFullName}</p>
      <p><strong>Créé le:</strong> ${new Date(this.contract.createdAt).toLocaleDateString('fr-FR')}</p>
      <hr>
      <p>Ce contrat régit la location de la propriété mentionnée ci-dessus.</p>
      <p>En signant électroniquement ce document, vous acceptez tous les termes et conditions.</p>
    `;
  }

  /**
   * Gère la signature depuis le dialogue
   */
  handleSign(signaturePayload: string): void {
    if (!this.contract) return;

    this.signing = true;
    const payload = JSON.parse(signaturePayload);

    // Extraire la partie Base64 de la signature (enlever le préfixe data:image/png;base64,)
    let signatureBase64 = payload.signature;
    if (signatureBase64.startsWith('data:')) {
      signatureBase64 = signatureBase64.split(',')[1];
    }

    const request: CreateSignatureRequest = {
      contractId: this.contract.id,
      signatureData: signatureBase64,
      ipAddress: 'frontend',
      userAgent: navigator.userAgent
    };

    this.signatureService.createSignature(request).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.signing = false)
    ).subscribe({
      next: (signature) => {
        console.log('Signature created successfully:', signature);
        this.showSignDialog = false;
        this.notificationService.success('Contrat signé avec succès!');

        // Recharger le contrat et les signatures
        this.loadContract(this.contract!.id);
        this.loadSignatures(this.contract!.id);
      },
      error: (err) => {
        console.error('Error creating signature:', err);
        this.notificationService.error(err.error?.message || 'Erreur lors de la signature du contrat');
      }
    });
  }

  /**
   * Ferme le dialogue de signature
   */
  closeSignDialog(): void {
    this.showSignDialog = false;
  }

  /**
   * Envoie le contrat pour signature (remplace DocuSign)
   */
  sendForSignature(): void {
    if (!this.contract) return;

    if (this.contract.status !== ContractStatus.DRAFT) {
      this.notificationService.warning('Seuls les contrats en brouillon peuvent être envoyés pour signature');
      return;
    }

    if (confirm('Envoyer ce contrat au locataire pour signature ?')) {
      const request: UpdateContractRequest = {
        status: ContractStatus.SENT
      };

      this.contractService.updateContract(this.contract.id, request).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: (updatedContract) => {
          this.contract = updatedContract;
          this.notificationService.success('Contrat envoyé avec succès ! Le locataire peut maintenant le signer.');
        },
        error: (err) => {
          console.error('Error sending contract:', err);
          this.notificationService.error(err.error?.message || 'Erreur lors de l\'envoi du contrat');
        }
      });
    }
  }

  // === E-Signature Methods ===

  /**
   * Charge la demande de signature électronique si elle existe
   */
  loadSignatureRequest(contractId: string): void {
    this.loadingSignatureRequest = true;

    this.esignatureService.getSignatureStatus(contractId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (request) => {
        this.signatureRequest = request;
        this.loadingSignatureRequest = false;
      },
      error: (err) => {
        // Pas de demande de signature, c'est normal
        this.loadingSignatureRequest = false;
      }
    });
  }

  /**
   * Envoie le contrat pour signature électronique
   */
  sendForESignature(): void {
    if (!this.contract) return;

    if (this.contract.status !== ContractStatus.DRAFT) {
      this.notificationService.warning('Seuls les contrats en brouillon peuvent être envoyés pour signature');
      return;
    }

    if (confirm('Envoyer ce contrat au locataire pour signature électronique via DocuSign ?')) {
      this.sendingForSignature = true;

      this.esignatureService.sendForSignature(this.contract.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: (request) => {
          this.signatureRequest = request;
          this.sendingForSignature = false;
          this.notificationService.success('Contrat envoyé pour signature électronique avec succès !');

          // Recharger le contrat pour voir le nouveau statut
          this.loadContract(this.contract!.id);
        },
        error: (err) => {
          console.error('Error sending for e-signature:', err);
          this.sendingForSignature = false;
          alert(err.error?.message || 'Erreur lors de l\'envoi pour signature');
        }
      });
    }
  }

  /**
   * Renvoie l'email de signature
   */
  resendSignatureRequest(): void {
    if (!this.contract || !this.signatureRequest) return;

    if (confirm('Renvoyer l\'email de signature au locataire ?')) {
      this.esignatureService.resendSignatureRequest(
        this.contract.id,
        this.signatureRequest.id
      ).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.notificationService.success('Email de signature renvoyé avec succès');
          this.loadSignatureRequest(this.contract!.id);
        },
        error: (err) => {
          console.error('Error resending signature request:', err);
          alert('Erreur lors du renvoi de l\'email');
        }
      });
    }
  }

  /**
   * Annule la demande de signature
   */
  cancelSignatureRequest(): void {
    if (!this.contract || !this.signatureRequest) return;

    if (confirm('Êtes-vous sûr de vouloir annuler cette demande de signature ?')) {
      this.esignatureService.cancelSignatureRequest(
        this.contract.id,
        this.signatureRequest.id
      ).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.notificationService.success('Demande de signature annulée');
          this.loadSignatureRequest(this.contract!.id);
          this.loadContract(this.contract!.id);
        },
        error: (err) => {
          console.error('Error cancelling signature request:', err);
          alert('Erreur lors de l\'annulation');
        }
      });
    }
  }

  /**
   * Vérifie si le bouton d'envoi pour e-signature doit être affiché
   */
  canSendForESignature(): boolean {
    return this.contract?.status === ContractStatus.DRAFT && !this.signatureRequest;
  }

  /**
   * Vérifie si les actions de renvoi/annulation sont disponibles
   */
  canManageSignatureRequest(): boolean {
    if (!this.signatureRequest) return false;
    return SignatureStatusHelper.canResend(this.signatureRequest.status) ||
           SignatureStatusHelper.canCancel(this.signatureRequest.status);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
