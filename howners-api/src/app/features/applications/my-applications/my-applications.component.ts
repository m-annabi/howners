import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
import { downloadBlob } from '../../../shared/utils/file.utils';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { DocumentService } from '../../../core/services/document.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantActionsService } from '../../../core/services/tenant-actions.service';
import {
  Application,
  ApplicationStatus,
  ReviewApplicationRequest,
  APPLICATION_STATUS_LABELS,
  APPLICATION_STATUS_COLORS
} from '../../../core/models/application.model';
import { Rental } from '../../../core/models/rental.model';
import { DocumentType } from '../../../core/models/document.model';

interface RequiredDocumentType {
  type: DocumentType;
  label: string;
}

/** Étape de la timeline de suivi d'une candidature (onglet locataire). */
interface TimelineStep {
  label: string;
  state: 'done' | 'current' | 'todo' | 'ko';
}

@Component({
  selector: 'app-my-applications',
  templateUrl: './my-applications.component.html',
  styleUrls: ['./my-applications.component.scss']
})
export class MyApplicationsComponent implements OnInit, OnDestroy {
  applications: Application[] = [];
  loading = false;
  error: string | null = null;
  submitting = false;
  activeTab: 'received' | 'sent' = 'sent';
  isOwner = false;

  // Review (owner)
  reviewingId: string | null = null;
  reviewNotes = '';
  expandedDossierId: string | null = null;

  // Create rental modal
  showCreateRentalModal = false;
  acceptedApplication: Application | null = null;

  private userSub!: Subscription;

  statusLabels = APPLICATION_STATUS_LABELS;
  statusColors = APPLICATION_STATUS_COLORS;

  readonly REQUIRED_DOCUMENT_TYPES: RequiredDocumentType[] = [
    { type: DocumentType.IDENTITY, label: 'Piece d\'identite' },
    { type: DocumentType.PROOF_OF_INCOME, label: 'Bulletins de salaire' },
    { type: DocumentType.EMPLOYMENT_CONTRACT, label: 'Contrat de travail' },
    { type: DocumentType.TAX_NOTICE, label: 'Avis d\'imposition' },
    { type: DocumentType.PROOF_OF_RESIDENCE, label: 'Justificatif de domicile' },
  ];

  constructor(
    private authService: AuthService,
    private applicationService: ApplicationService,
    private documentService: DocumentService,
    private router: Router,
    private notificationService: NotificationService,
    private confirmDialog: ConfirmDialogService,
    private tenantActionsService: TenantActionsService
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.isOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';
      this.activeTab = this.isOwner ? 'received' : 'sent';
      this.loadApplications();
    });
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  switchTab(tab: 'received' | 'sent'): void {
    this.activeTab = tab;
    this.reviewingId = null;
    this.expandedDossierId = null;
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.error = null;
    const obs = this.activeTab === 'received'
      ? this.applicationService.getReceivedApplications()
      : this.applicationService.getMyApplications();

    obs.subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des candidatures';
        this.loading = false;
      }
    });
  }

  // --- Tenant actions ---
  withdraw(id: string): void {
    this.confirmDialog.confirm('Confirmation', 'Retirer cette candidature ?', 'danger').subscribe(ok => {
      if (!ok) return;
      this.applicationService.withdraw(id).subscribe({
        next: () => {
          this.notificationService.success('Candidature retirée');
          this.loadApplications();
          this.tenantActionsService.refresh();
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors du retrait de la candidature';
        }
      });
    });
  }

  canWithdraw(app: Application): boolean {
    return app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW';
  }

  /**
   * Timeline de suivi (onglet locataire) : les étapes contrat sont dérivées du statut du
   * contrat lié (exposé par le back après acceptation), sans nouvel état de candidature.
   */
  timelineFor(app: Application): TimelineStep[] {
    if (app.status === 'WITHDRAWN') {
      return [
        { label: 'Envoyée', state: 'done' },
        { label: 'Retirée', state: 'ko' }
      ];
    }
    if (app.status === 'REJECTED') {
      return [
        { label: 'Envoyée', state: 'done' },
        { label: 'Examinée', state: 'done' },
        { label: 'Refusée', state: 'ko' }
      ];
    }
    const accepted = app.status === 'ACCEPTED';
    const contractSigned = app.contractStatus === 'SIGNED' || app.contractStatus === 'ACTIVE';
    const contractSent = app.contractStatus === 'SENT';
    return [
      { label: 'Envoyée', state: 'done' },
      { label: 'En examen', state: accepted ? 'done' : 'current' },
      { label: 'Acceptée', state: accepted ? 'done' : 'todo' },
      { label: 'Contrat à signer', state: contractSigned ? 'done' : (contractSent ? 'current' : 'todo') },
      { label: 'Contrat signé', state: contractSigned ? 'done' : 'todo' }
    ];
  }

  /** CTA « Signer mon contrat » : contrat envoyé, en attente de la signature du locataire. */
  mustSignContract(app: Application): boolean {
    return !!app.contractId && app.contractStatus === 'SENT';
  }

  // --- Owner actions ---
  startReview(id: string): void {
    this.reviewingId = id;
    this.reviewNotes = '';
  }

  cancelReview(): void {
    this.reviewingId = null;
    this.reviewNotes = '';
  }

  accept(id: string): void {
    this.submitting = true;
    this.error = null;
    const request: ReviewApplicationRequest = {
      status: ApplicationStatus.ACCEPTED,
      notes: this.reviewNotes || undefined
    };
    this.applicationService.review(id, request).subscribe({
      next: (updatedApp) => {
        this.submitting = false;
        this.reviewingId = null;
        this.reviewNotes = '';
        this.acceptedApplication = updatedApp;
        this.showCreateRentalModal = true;
        this.loadApplications();
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.message || 'Erreur lors de l\'acceptation de la candidature';
      }
    });
  }

  reject(id: string): void {
    this.review(id, ApplicationStatus.REJECTED);
  }

  private review(id: string, status: ApplicationStatus): void {
    this.submitting = true;
    this.error = null;
    const request: ReviewApplicationRequest = {
      status,
      notes: this.reviewNotes || undefined
    };
    this.applicationService.review(id, request).subscribe({
      next: () => {
        this.submitting = false;
        this.reviewingId = null;
        this.reviewNotes = '';
        this.notificationService.success(
          status === ApplicationStatus.REJECTED ? 'Candidature refusée' : 'Candidature mise à jour'
        );
        this.loadApplications();
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.message || 'Erreur lors du traitement de la candidature';
      }
    });
  }

  canReview(app: Application): boolean {
    return app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW';
  }

  // --- Create rental modal ---
  openCreateRentalModal(app: Application): void {
    this.acceptedApplication = app;
    this.showCreateRentalModal = true;
  }

  onRentalCreated(rental: Rental): void {
    this.showCreateRentalModal = false;
    this.acceptedApplication = null;
    this.router.navigate(['/contracts/new'], { queryParams: { rentalId: rental.id } });
  }

  onRentalModalCancelled(): void {
    this.showCreateRentalModal = false;
    this.acceptedApplication = null;
  }

  toggleDossier(appId: string): void {
    this.expandedDossierId = this.expandedDossierId === appId ? null : appId;
  }

  // --- Shared ---
  getDocumentCount(app: Application): number {
    if (!app.documents) return 0;
    const types = new Set(app.documents.map(d => d.documentType));
    return this.REQUIRED_DOCUMENT_TYPES.filter(r => types.has(r.type)).length;
  }

  hasDocumentType(app: Application, type: DocumentType): boolean {
    return app.documents?.some(d => d.documentType === type) ?? false;
  }

  isDossierComplete(app: Application): boolean {
    return this.getDocumentCount(app) === this.REQUIRED_DOCUMENT_TYPES.length;
  }

  downloadDocument(docId: string, fileName: string): void {
    this.documentService.downloadDocument(docId).subscribe({
      next: (blob) => downloadBlob(blob, fileName)
    });
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}
