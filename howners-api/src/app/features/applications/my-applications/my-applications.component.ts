import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { DocumentService } from '../../../core/services/document.service';
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

@Component({
  selector: 'app-my-applications',
  templateUrl: './my-applications.component.html'
})
export class MyApplicationsComponent implements OnInit {
  applications: Application[] = [];
  loading = false;
  activeTab: 'received' | 'sent' = 'sent';
  isOwner = false;

  // Review (owner)
  reviewingId: string | null = null;
  reviewNotes = '';
  expandedDossierId: string | null = null;

  // Create rental modal
  showCreateRentalModal = false;
  acceptedApplication: Application | null = null;

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
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.isOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';
      this.activeTab = this.isOwner ? 'received' : 'sent';
      this.loadApplications();
    });
  }

  switchTab(tab: 'received' | 'sent'): void {
    this.activeTab = tab;
    this.reviewingId = null;
    this.expandedDossierId = null;
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    const obs = this.activeTab === 'received'
      ? this.applicationService.getReceivedApplications()
      : this.applicationService.getMyApplications();

    obs.subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // --- Tenant actions ---
  withdraw(id: string): void {
    if (confirm('Retirer cette candidature ?')) {
      this.applicationService.withdraw(id).subscribe(() => this.loadApplications());
    }
  }

  canWithdraw(app: Application): boolean {
    return app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW';
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
    const request: ReviewApplicationRequest = {
      status: ApplicationStatus.ACCEPTED,
      notes: this.reviewNotes || undefined
    };
    this.applicationService.review(id, request).subscribe({
      next: (updatedApp) => {
        this.reviewingId = null;
        this.reviewNotes = '';
        this.acceptedApplication = updatedApp;
        this.showCreateRentalModal = true;
        this.loadApplications();
      }
    });
  }

  reject(id: string): void {
    this.review(id, ApplicationStatus.REJECTED);
  }

  private review(id: string, status: ApplicationStatus): void {
    const request: ReviewApplicationRequest = {
      status,
      notes: this.reviewNotes || undefined
    };
    this.applicationService.review(id, request).subscribe({
      next: () => {
        this.reviewingId = null;
        this.reviewNotes = '';
        this.loadApplications();
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
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}
