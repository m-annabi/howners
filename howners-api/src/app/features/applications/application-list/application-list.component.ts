import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { DocumentService } from '../../../core/services/document.service';

import {
  Application,
  ApplicationStatus,
  ReviewApplicationRequest,
  APPLICATION_STATUS_LABELS,
  APPLICATION_STATUS_COLORS
} from '../../../core/models/application.model';
import { DocumentType } from '../../../core/models/document.model';

interface RequiredDocumentType {
  type: DocumentType;
  label: string;
}

@Component({
  selector: 'app-application-list',
  templateUrl: './application-list.component.html'
})
export class ApplicationListComponent implements OnInit {
  applications: Application[] = [];
  loading = false;
  error: string | null = null;
  listingId: string | null = null;
  reviewingId: string | null = null;
  reviewNotes = '';
  expandedDossierId: string | null = null;

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
    private applicationService: ApplicationService,
    private documentService: DocumentService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.listingId = this.route.snapshot.queryParamMap.get('listingId');
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.error = null;
    const obs = this.listingId
      ? this.applicationService.getByListing(this.listingId)
      : this.applicationService.getReceivedApplications();

    obs.subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des candidatures';
        this.loading = false;
      }
    });
  }

  startReview(id: string): void {
    this.reviewingId = id;
    this.reviewNotes = '';
  }

  cancelReview(): void {
    this.reviewingId = null;
    this.reviewNotes = '';
  }

  accept(id: string): void {
    this.review(id, ApplicationStatus.ACCEPTED);
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
      },
      error: () => {
        this.error = 'Erreur lors du traitement de la candidature';
      }
    });
  }

  canReview(app: Application): boolean {
    return app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW';
  }

  toggleDossier(appId: string): void {
    this.expandedDossierId = this.expandedDossierId === appId ? null : appId;
  }

  getDocumentCount(app: Application): number {
    if (!app.documents) return 0;
    const types = new Set(app.documents.map(d => d.documentType));
    return this.REQUIRED_DOCUMENT_TYPES.filter(r => types.has(r.type)).length;
  }

  hasDocumentType(app: Application, type: DocumentType): boolean {
    return app.documents?.some(d => d.documentType === type) ?? false;
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
      },
      error: () => {
        this.error = 'Erreur lors du telechargement du document';
      }
    });
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}
