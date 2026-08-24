import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DocumentService } from '../../../core/services/document.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Document, DOCUMENT_TYPE_LABELS } from '../../../core/models/document.model';

@Component({
  selector: 'app-document-list',
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css']
})
export class DocumentListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private currentUserId: string | null = null;

  @Input() propertyId?: string;
  @Input() rentalId?: string;
  @Input() showUpload = true;
  @Input() showArchiveActions = false;
  /**
   * Droit de supprimer *tous* les documents du dossier (propriétaire du bien).
   * À false, chacun ne peut retirer que les fichiers qu'il a lui-même déposés —
   * c'est la règle appliquée par le back dans DocumentService.deleteDocument().
   */
  @Input() allowDeleteAll = true;

  /** Nombre de lignes affichées avant de devoir dérouler la liste. */
  @Input() pageSize = 6;

  documents: Document[] = [];
  loading = false;
  error: string | null = null;
  showUploadForm = false;
  retentionDocId: string | null = null;
  retentionDate = '';

  /** Filtres de lecture : type sélectionné et recherche sur le nom du fichier. */
  filterType = '';
  search = '';
  expanded = false;

  types: { value: string; label: string; count: number }[] = [];
  filtered: Document[] = [];
  visible: Document[] = [];

  documentTypeLabels = DOCUMENT_TYPE_LABELS;

  constructor(
    public documentService: DocumentService,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$))
      .subscribe(user => this.currentUserId = user?.id ?? null);
    this.loadDocuments();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Un document se supprime si on gère le dossier, ou si on l'a déposé soi-même :
   * le locataire peut ainsi retirer un justificatif envoyé par erreur, sans
   * toucher aux pièces émises par le propriétaire (quittances, contrats…).
   */
  canDelete(document: Document): boolean {
    if (document.isArchived) return false;
    return this.allowDeleteAll || document.uploaderId === this.currentUserId;
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = null;

    let observable;

    if (this.propertyId) {
      observable = this.documentService.getPropertyDocuments(this.propertyId);
    } else if (this.rentalId) {
      observable = this.documentService.getRentalDocuments(this.rentalId);
    } else {
      observable = this.documentService.getMyDocuments();
    }

    observable.subscribe({
      next: (documents) => {
        // Plus récent en premier : c'est ce qu'on vient chercher sur un bail actif.
        this.documents = [...documents].sort((a, b) =>
          new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime());
        this.buildTypeFilter();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des documents';
        this.loading = false;
      }
    });
  }

  /** Types réellement présents dans la liste, avec leur nombre. */
  private buildTypeFilter(): void {
    const counts = new Map<string, number>();
    this.documents.forEach(d => counts.set(d.documentType, (counts.get(d.documentType) || 0) + 1));

    this.types = [...counts.entries()]
      .map(([value, count]) => ({ value, label: this.getDocumentTypeLabel(value), count }))
      .sort((a, b) => a.label.localeCompare(b.label));

    if (this.filterType && !counts.has(this.filterType)) {
      this.filterType = '';
    }
  }

  /** Recalcule la liste visible après un changement de filtre ou de recherche. */
  applyFilters(): void {
    const q = this.search.trim().toLowerCase();

    this.filtered = this.documents.filter(d => {
      if (this.filterType && d.documentType !== this.filterType) return false;
      if (!q) return true;
      return d.fileName.toLowerCase().includes(q)
          || (d.description || '').toLowerCase().includes(q);
    });

    this.visible = this.expanded ? this.filtered : this.filtered.slice(0, this.pageSize);
  }

  onFilterChange(): void {
    this.expanded = false;
    this.applyFilters();
  }

  toggleExpanded(): void {
    this.expanded = !this.expanded;
    this.applyFilters();
  }

  get hiddenCount(): number {
    return this.filtered.length - this.visible.length;
  }

  toggleUploadForm(): void {
    this.showUploadForm = !this.showUploadForm;
  }

  onUploadComplete(): void {
    this.showUploadForm = false;
    this.loadDocuments();
  }

  downloadDocument(doc: Document): void {
    this.documentService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = window.document.createElement('a');
        a.href = url;
        a.download = doc.fileName;
        window.document.body.appendChild(a);
        a.click();
        window.document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.notificationService.error('Erreur lors du téléchargement du document');
      }
    });
  }

  deleteDocument(document: Document, event: Event): void {
    event.stopPropagation();

    if (confirm(`Êtes-vous sûr de vouloir supprimer ${document.fileName} ?`)) {
      this.documentService.deleteDocument(document.id).subscribe({
        next: () => {
          this.loadDocuments();
        },
        error: () => {
          this.notificationService.error('Erreur lors de la suppression du document');
        }
      });
    }
  }

  getDocumentTypeLabel(type: string): string {
    return this.documentTypeLabels[type as keyof typeof this.documentTypeLabels] || type;
  }

  archiveDocument(doc: Document, event: Event): void {
    event.stopPropagation();
    if (confirm(`Archiver le document ${doc.fileName} ?`)) {
      this.documentService.archiveDocument(doc.id).subscribe({
        next: () => this.loadDocuments(),
        error: () => {
          this.notificationService.error('Erreur lors de l\'archivage du document');
        }
      });
    }
  }

  toggleLegalHold(doc: Document, event: Event): void {
    event.stopPropagation();
    const action = doc.legalHold ? 'retirer le blocage légal de' : 'mettre un blocage légal sur';
    if (confirm(`${action} ${doc.fileName} ?`)) {
      this.documentService.setLegalHold(doc.id, !doc.legalHold).subscribe({
        next: () => this.loadDocuments(),
        error: () => {
          this.notificationService.error('Erreur lors de la modification du blocage légal');
        }
      });
    }
  }

  startSetRetention(docId: string): void {
    this.retentionDocId = docId;
    this.retentionDate = '';
  }

  cancelRetention(): void {
    this.retentionDocId = null;
    this.retentionDate = '';
  }

  saveRetention(): void {
    if (!this.retentionDocId || !this.retentionDate) return;
    this.documentService.setRetention(this.retentionDocId, this.retentionDate).subscribe({
      next: () => {
        this.retentionDocId = null;
        this.retentionDate = '';
        this.loadDocuments();
      },
      error: () => {
        this.notificationService.error('Erreur lors de la définition de la rétention');
      }
    });
  }
}
