import { Component, OnInit } from '@angular/core';
import { DocumentService } from '../../../core/services/document.service';
import { Document, DocumentType } from '../../../core/models/document.model';

interface DossierPiece {
  type: DocumentType;
  label: string;
  description: string;
  icon: string;
  docs: Document[];
  uploading: boolean;
}

@Component({
  selector: 'app-tenant-dossier',
  templateUrl: './tenant-dossier.component.html',
  styleUrls: ['./tenant-dossier.component.scss']
})
export class TenantDossierComponent implements OnInit {
  pieces: DossierPiece[] = [
    {
      type: DocumentType.IDENTITY,
      label: "Pièce d'identité",
      description: 'CNI, passeport ou titre de séjour (recto-verso)',
      icon: 'bi-person-badge',
      docs: [],
      uploading: false
    },
    {
      type: DocumentType.PROOF_OF_INCOME,
      label: 'Bulletins de salaire',
      description: '3 derniers bulletins de salaire',
      icon: 'bi-cash-stack',
      docs: [],
      uploading: false
    },
    {
      type: DocumentType.EMPLOYMENT_CONTRACT,
      label: 'Contrat de travail',
      description: "Contrat de travail ou attestation employeur",
      icon: 'bi-briefcase',
      docs: [],
      uploading: false
    },
    {
      type: DocumentType.TAX_NOTICE,
      label: "Avis d'imposition",
      description: "Dernier avis d'imposition",
      icon: 'bi-file-earmark-text',
      docs: [],
      uploading: false
    },
    {
      type: DocumentType.PROOF_OF_RESIDENCE,
      label: 'Justificatif de domicile',
      description: 'Facture récente (- 3 mois) ou quittance de loyer',
      icon: 'bi-house',
      docs: [],
      uploading: false
    }
  ];

  loading = true;
  error: string | null = null;
  uploadError: string | null = null;

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  private loadDocuments(): void {
    this.loading = true;
    this.documentService.getMyDocuments().subscribe({
      next: (docs) => {
        const dossierTypes = new Set(this.pieces.map(p => p.type));
        const dossierDocs = docs.filter(d => dossierTypes.has(d.documentType));
        this.pieces.forEach(p => {
          p.docs = dossierDocs.filter(d => d.documentType === p.type);
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger vos documents.';
        this.loading = false;
      }
    });
  }

  onFileSelected(event: Event, piece: DossierPiece): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';

    if (file.size > 10 * 1024 * 1024) {
      this.uploadError = 'Le fichier ne doit pas dépasser 10 Mo.';
      return;
    }

    this.uploadError = null;
    piece.uploading = true;

    this.documentService.uploadDocument({ file, documentType: piece.type }).subscribe({
      next: (doc) => {
        piece.docs.push(doc);
        piece.uploading = false;
      },
      error: () => {
        this.uploadError = "Erreur lors de l'envoi du fichier.";
        piece.uploading = false;
      }
    });
  }

  removeDoc(piece: DossierPiece, doc: Document): void {
    this.documentService.deleteDocument(doc.id).subscribe({
      next: () => {
        piece.docs = piece.docs.filter(d => d.id !== doc.id);
      },
      error: () => {
        this.uploadError = 'Impossible de supprimer ce document.';
      }
    });
  }

  downloadDoc(doc: Document): void {
    this.documentService.downloadDocument(doc.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = doc.fileName;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  get completedCount(): number {
    return this.pieces.filter(p => p.docs.length > 0).length;
  }

  get totalCount(): number {
    return this.pieces.length;
  }

  get completionPercent(): number {
    return Math.round((this.completedCount / this.totalCount) * 100);
  }

  get isComplete(): boolean {
    return this.completedCount === this.totalCount;
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}
