import { Component, Input, OnInit } from '@angular/core';
import { DocumentService } from '../../../core/services/document.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Document, DOCUMENT_TYPE_LABELS } from '../../../core/models/document.model';

@Component({
  selector: 'app-document-list',
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css']
})
export class DocumentListComponent implements OnInit {
  @Input() propertyId?: string;
  @Input() rentalId?: string;
  @Input() showUpload = true;
  @Input() showArchiveActions = false;

  documents: Document[] = [];
  loading = false;
  error: string | null = null;
  showUploadForm = false;
  retentionDocId: string | null = null;
  retentionDate = '';

  documentTypeLabels = DOCUMENT_TYPE_LABELS;

  constructor(
    public documentService: DocumentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadDocuments();
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
        this.documents = documents;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        this.error = 'Erreur lors du chargement des documents';
        this.loading = false;
      }
    });
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
      error: (err) => {
        console.error('Error downloading document:', err);
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
        error: (err) => {
          console.error('Error deleting document:', err);
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
        error: (err) => {
          console.error('Error archiving document:', err);
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
        error: (err) => {
          console.error('Error toggling legal hold:', err);
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
      error: (err) => {
        console.error('Error setting retention:', err);
        this.notificationService.error('Erreur lors de la définition de la rétention');
      }
    });
  }
}
