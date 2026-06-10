import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { DocumentService } from '../../../core/services/document.service';
import { ListingService } from '../../../core/services/listing.service';
import { Listing } from '../../../core/models/listing.model';
import { Document, DocumentType, DOCUMENT_TYPE_LABELS } from '../../../core/models/document.model';

interface RequiredDocument {
  type: DocumentType;
  label: string;
  description: string;
}

@Component({
  selector: 'app-application-form',
  templateUrl: './application-form.component.html'
})
export class ApplicationFormComponent implements OnInit {
  form!: FormGroup;
  listing: Listing | null = null;
  loading = true;
  submitting = false;

  // Etape 2: Upload des documents
  applicationId: string | null = null;
  uploadedDocuments: Document[] = [];
  uploadingType: DocumentType | null = null;
  uploadError: string | null = null;

  readonly REQUIRED_DOCUMENTS: RequiredDocument[] = [
    { type: DocumentType.IDENTITY, label: 'Piece d\'identite', description: 'CNI, passeport ou titre de sejour' },
    { type: DocumentType.PROOF_OF_INCOME, label: 'Bulletins de salaire (3 derniers)', description: '3 derniers bulletins de salaire' },
    { type: DocumentType.EMPLOYMENT_CONTRACT, label: 'Contrat de travail', description: 'Contrat de travail ou attestation employeur' },
    { type: DocumentType.TAX_NOTICE, label: 'Avis d\'imposition', description: 'Dernier avis d\'imposition' },
    { type: DocumentType.PROOF_OF_RESIDENCE, label: 'Justificatif de domicile', description: 'Facture recente ou quittance de loyer' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private documentService: DocumentService,
    private listingService: ListingService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      listingId: ['', Validators.required],
      coverLetter: [''],
      desiredMoveIn: [null]
    });

    const listingId = this.route.snapshot.queryParamMap.get('listingId');
    if (listingId) {
      this.form.patchValue({ listingId });
      this.listingService.getListing(listingId).subscribe({
        next: (listing) => {
          this.listing = listing;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.router.navigate(['/listings']);
        }
      });
    } else {
      this.loading = false;
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.submitting = true;
    this.applicationService.submit(this.form.value).subscribe({
      next: (application) => {
        this.submitting = false;
        this.applicationId = application.id;
      },
      error: () => this.submitting = false
    });
  }

  onFileSelected(event: Event, docType: DocumentType): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    if (file.size > 10 * 1024 * 1024) {
      this.uploadError = 'Le fichier ne doit pas depasser 10 Mo';
      input.value = '';
      return;
    }

    this.uploadError = null;
    this.uploadingType = docType;

    this.documentService.uploadDocument({
      file,
      documentType: docType,
      applicationId: this.applicationId!
    }).subscribe({
      next: (doc) => {
        this.uploadedDocuments.push(doc);
        this.uploadingType = null;
        input.value = '';
      },
      error: () => {
        this.uploadError = 'Erreur lors de l\'upload du document';
        this.uploadingType = null;
        input.value = '';
      }
    });
  }

  removeDocument(doc: Document): void {
    this.documentService.deleteDocument(doc.id).subscribe({
      next: () => {
        this.uploadedDocuments = this.uploadedDocuments.filter(d => d.id !== doc.id);
      },
      error: () => {
        this.uploadError = 'Erreur lors de la suppression du document';
      }
    });
  }

  getDocumentsForType(type: DocumentType): Document[] {
    return this.uploadedDocuments.filter(d => d.documentType === type);
  }

  isTypeUploaded(type: DocumentType): boolean {
    return this.uploadedDocuments.some(d => d.documentType === type);
  }

  get uploadedCount(): number {
    const uploadedTypes = new Set(this.uploadedDocuments.map(d => d.documentType));
    return this.REQUIRED_DOCUMENTS.filter(r => uploadedTypes.has(r.type)).length;
  }

  get totalRequired(): number {
    return this.REQUIRED_DOCUMENTS.length;
  }

  get isComplete(): boolean {
    return this.uploadedCount === this.totalRequired;
  }

  finish(): void {
    this.router.navigate(['/applications']);
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}
