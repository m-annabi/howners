import { Component, OnInit } from '@angular/core';
import { AuditService } from '../../../core/services/audit.service';
import {
  AuditLog,
  AuditAction,
  AUDIT_ACTION_LABELS
} from '../../../core/models/audit.model';

@Component({
  selector: 'app-audit-log-list',
  templateUrl: './audit-log-list.component.html',
  styles: []
})
export class AuditLogListComponent implements OnInit {
  logs: AuditLog[] = [];
  loading = false;
  error: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  // Filters
  selectedEntityType = '';
  selectedAction: AuditAction | '' = '';
  selectedUserId = '';

  actionLabels = AUDIT_ACTION_LABELS;
  AuditAction = AuditAction;

  actions = [
    { value: '', label: 'Toutes les actions' },
    ...Object.values(AuditAction).map(a => ({ value: a, label: AUDIT_ACTION_LABELS[a] }))
  ];

  entityTypes = [
    { value: '', label: 'Tous les types' },
    { value: 'User', label: 'Utilisateur' },
    { value: 'Rental', label: 'Location' },
    { value: 'Contract', label: 'Contrat' },
    { value: 'Payment', label: 'Paiement' },
    { value: 'Property', label: 'Propriété' }
  ];

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.error = null;

    this.auditService.getAll(
      this.currentPage,
      this.pageSize,
      this.selectedEntityType || undefined,
      this.selectedAction || undefined,
      this.selectedUserId || undefined
    ).subscribe({
      next: (page) => {
        this.logs = page.content;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading audit logs:', err);
        this.error = 'Erreur lors du chargement des logs d\'audit';
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadLogs();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadLogs();
    }
  }

  getActionLabel(action: AuditAction): string {
    return this.actionLabels[action] || action;
  }

  getActionColor(action: AuditAction): string {
    switch (action) {
      case AuditAction.CREATE: return 'success';
      case AuditAction.DELETE: return 'danger';
      case AuditAction.UPDATE: return 'warning';
      case AuditAction.LOGIN: return 'info';
      case AuditAction.PAYMENT_CONFIRMED: return 'success';
      case AuditAction.DATA_ERASURE: return 'danger';
      default: return 'secondary';
    }
  }
}
