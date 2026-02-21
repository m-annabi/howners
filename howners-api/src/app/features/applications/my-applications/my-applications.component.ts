import { Component, OnInit } from '@angular/core';
import { ApplicationService } from '../../../core/services/application.service';
import { Application, APPLICATION_STATUS_LABELS, APPLICATION_STATUS_COLORS } from '../../../core/models/application.model';
import { DocumentType } from '../../../core/models/document.model';

@Component({
  selector: 'app-my-applications',
  templateUrl: './my-applications.component.html'
})
export class MyApplicationsComponent implements OnInit {
  applications: Application[] = [];
  loading = false;

  statusLabels = APPLICATION_STATUS_LABELS;
  statusColors = APPLICATION_STATUS_COLORS;

  private readonly REQUIRED_TYPES = [
    DocumentType.IDENTITY,
    DocumentType.PROOF_OF_INCOME,
    DocumentType.EMPLOYMENT_CONTRACT,
    DocumentType.TAX_NOTICE,
    DocumentType.PROOF_OF_RESIDENCE,
  ];

  constructor(private applicationService: ApplicationService) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.applicationService.getMyApplications().subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  withdraw(id: string): void {
    if (confirm('Retirer cette candidature ?')) {
      this.applicationService.withdraw(id).subscribe(() => this.loadApplications());
    }
  }

  canWithdraw(app: Application): boolean {
    return app.status === 'SUBMITTED' || app.status === 'UNDER_REVIEW';
  }

  getDocumentCount(app: Application): number {
    if (!app.documents) return 0;
    const types = new Set(app.documents.map(d => d.documentType));
    return this.REQUIRED_TYPES.filter(t => types.has(t)).length;
  }

  get totalRequired(): number {
    return this.REQUIRED_TYPES.length;
  }

  isDossierComplete(app: Application): boolean {
    return this.getDocumentCount(app) === this.totalRequired;
  }
}
