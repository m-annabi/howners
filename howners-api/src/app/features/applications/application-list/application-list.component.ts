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
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

interface RequiredDocumentType {
  type: DocumentType;
  label: string;
}

@Component({
  selector: 'app-application-list',
  templateUrl: './application-list.component.html',
  styleUrls: ['./application-list.component.scss']
})
export class ApplicationListComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  loading = false;
  error: string | null = null;
  listingId: string | null = null;
  reviewingId: string | null = null;
  reviewNotes = '';
  expandedDossierId: string | null = null;
  activeFilter: string = 'PENDING';

  statusLabels = APPLICATION_STATUS_LABELS;
  statusColors = APPLICATION_STATUS_COLORS;

  get filters(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('ALL', this.applications.length);
    let pending = 0;
    for (const a of this.applications) {
      counts.set(a.status, (counts.get(a.status) || 0) + 1);
      if (a.status === ApplicationStatus.SUBMITTED || a.status === ApplicationStatus.UNDER_REVIEW) {
        pending++;
      }
    }
    const list: QuickFilter[] = [
      { key: 'PENDING', label: 'À examiner', count: pending, tone: 'warning' },
      { key: 'ALL', label: 'Toutes', count: counts.get('ALL') || 0 },
      { key: ApplicationStatus.ACCEPTED, label: 'Acceptées', count: counts.get(ApplicationStatus.ACCEPTED) || 0, tone: 'success' },
      { key: ApplicationStatus.REJECTED, label: 'Refusées', count: counts.get(ApplicationStatus.REJECTED) || 0 },
      { key: ApplicationStatus.WITHDRAWN, label: 'Retirées', count: counts.get(ApplicationStatus.WITHDRAWN) || 0 }
    ];
    return list.filter(f => f.key === 'PENDING' || f.key === 'ALL' || (f.count || 0) > 0);
  }

  onFilterChange(key: string): void {
    this.activeFilter = key;
    this.applyFilters();
  }

  applyFilters(): void {
    if (this.activeFilter === 'PENDING') {
      this.filteredApplications = this.applications.filter(a =>
        a.status === ApplicationStatus.SUBMITTED || a.status === ApplicationStatus.UNDER_REVIEW
      );
    } else if (this.activeFilter === 'ALL') {
      this.filteredApplications = this.applications;
    } else {
      this.filteredApplications = this.applications.filter(a => a.status === this.activeFilter);
    }
  }

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
    const filter = this.route.snapshot.queryParamMap.get('filter');
    if (filter === 'pending') this.activeFilter = 'PENDING';
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
        this.applyFilters();
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

  // Backwards-compat for template references to `applications` count
  hasFilteredApplications(): boolean {
    return this.filteredApplications.length > 0;
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
