import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DocumentService } from '../../../core/services/document.service';
import {
  DocumentType,
  DOCUMENT_TYPE_LABELS,
  UploadDocumentRequest
} from '../../../core/models/document.model';

@Component({
  selector: 'app-document-upload',
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css']
})
export class DocumentUploadComponent {
  @Input() propertyId?: string;
  @Input() rentalId?: string;
  @Output() uploadComplete = new EventEmitter<void>();

  selectedFile: File | null = null;
  selectedDocumentType: DocumentType = DocumentType.OTHER;
  description = '';
  uploading = false;
  error: string | null = null;

  // Enums et constantes pour le template
  DocumentType = DocumentType;
  documentTypeLabels = DOCUMENT_TYPE_LABELS;
  documentTypes = Object.keys(DocumentType).map(key => ({
    value: DocumentType[key as keyof typeof DocumentType],
    label: DOCUMENT_TYPE_LABELS[DocumentType[key as keyof typeof DocumentType]]
  }));

  constructor(private documentService: DocumentService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Valider la taille (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.error = 'Le fichier ne doit pas dépasser 10MB';
        this.selectedFile = null;
        return;
      }

      this.selectedFile = file;
      this.error = null;
    }
  }

  upload(): void {
    if (!this.selectedFile) {
      this.error = 'Veuillez sélectionner un fichier';
      return;
    }

    this.uploading = true;
    this.error = null;

    const request: UploadDocumentRequest = {
      file: this.selectedFile,
      documentType: this.selectedDocumentType,
      propertyId: this.propertyId,
      rentalId: this.rentalId,
      description: this.description || undefined
    };

    this.documentService.uploadDocument(request).subscribe({
      next: () => {
        this.uploading = false;
        this.selectedFile = null;
        this.description = '';
        this.uploadComplete.emit();

        // Réinitialiser le file input
        const fileInput = document.getElementById('fileInput') as HTMLInputElement;
        if (fileInput) {
          fileInput.value = '';
        }
      },
      error: (err) => {
        console.error('Error uploading document:', err);
        this.error = err.error?.message || 'Erreur lors de l\'upload du document';
        this.uploading = false;
      }
    });
  }

  cancel(): void {
    this.selectedFile = null;
    this.description = '';
    this.error = null;

    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  getFileSize(): string {
    if (!this.selectedFile) return '';
    return this.documentService.formatFileSize(this.selectedFile.size);
  }
}
