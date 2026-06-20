import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApplicationService } from '../../../core/services/application.service';
import { DocumentService } from '../../../core/services/document.service';
import { ListingService } from '../../../core/services/listing.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Listing } from '../../../core/models/listing.model';
import { ApplicationStatus } from '../../../core/models/application.model';
import { DocumentType } from '../../../core/models/document.model';

interface DossierPieceStatus {
  type: DocumentType;
  label: string;
  present: boolean;
}

type PageState = 'loading' | 'no-listing' | 'already-applied' | 'incomplete' | 'ready' | 'submitting' | 'error';

@Component({
  selector: 'app-application-form',
  templateUrl: './application-form.component.html',
  styleUrls: ['./application-form.component.scss']
})
export class ApplicationFormComponent implements OnInit {
  state: PageState = 'loading';
  listing: Listing | null = null;
  dossierPieces: DossierPieceStatus[] = [];
  form!: FormGroup;
  errorMessage: string | null = null;
  messageLength = 0;
  readonly MESSAGE_MAX = 300;

  private readonly REQUIRED_PIECES: { type: DocumentType; label: string }[] = [
    { type: DocumentType.IDENTITY,            label: "Pièce d'identité" },
    { type: DocumentType.PROOF_OF_INCOME,     label: 'Bulletins de salaire' },
    { type: DocumentType.EMPLOYMENT_CONTRACT, label: 'Contrat de travail' },
    { type: DocumentType.TAX_NOTICE,          label: "Avis d'imposition" },
    { type: DocumentType.PROOF_OF_RESIDENCE,  label: 'Justificatif de domicile' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private documentService: DocumentService,
    private listingService: ListingService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      desiredMoveIn: [null],
      message: ['']
    });

    this.form.get('message')!.valueChanges.subscribe(v => {
      this.messageLength = (v || '').length;
      if (this.messageLength > this.MESSAGE_MAX) {
        this.form.get('message')!.setValue((v as string).slice(0, this.MESSAGE_MAX), { emitEvent: false });
        this.messageLength = this.MESSAGE_MAX;
      }
    });

    const listingId = this.route.snapshot.queryParamMap.get('listingId');
    if (!listingId) {
      this.state = 'no-listing';
      return;
    }

    forkJoin({
      listing: this.listingService.getListing(listingId).pipe(catchError(() => of(null))),
      docs: this.documentService.getMyDocuments().pipe(catchError(() => of([]))),
      myApplications: this.applicationService.getMyApplications().pipe(catchError(() => of([])))
    }).subscribe(({ listing, docs, myApplications }) => {
      if (!listing) {
        this.state = 'no-listing';
        return;
      }
      this.listing = listing;

      const activeApplication = myApplications.find(
        a => a.listingId === listingId && a.status !== ApplicationStatus.WITHDRAWN
      );
      if (activeApplication) {
        this.state = 'already-applied';
        return;
      }

      const uploadedTypes = new Set(docs.map(d => d.documentType));
      this.dossierPieces = this.REQUIRED_PIECES.map(p => ({
        ...p,
        present: uploadedTypes.has(p.type)
      }));

      this.state = this.dossierPieces.every(p => p.present) ? 'ready' : 'incomplete';
    });
  }

  get missingPieces(): DossierPieceStatus[] {
    return this.dossierPieces.filter(p => !p.present);
  }

  get completedCount(): number {
    return this.dossierPieces.filter(p => p.present).length;
  }

  onSubmit(): void {
    if (this.state !== 'ready') return;
    this.state = 'submitting';
    this.errorMessage = null;

    const { desiredMoveIn, message } = this.form.value;
    this.applicationService.submit({
      listingId: this.listing!.id,
      coverLetter: message?.trim() || undefined,
      desiredMoveIn: desiredMoveIn || undefined
    }).subscribe({
      next: () => {
        this.notificationService.success('Candidature envoyée avec succès !');
        this.router.navigate(['/applications']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || "Erreur lors de l'envoi de la candidature.";
        this.state = 'ready';
      }
    });
  }

  goToDossier(): void {
    this.router.navigate(['/tenant/dossier']);
  }

  cancel(): void {
    this.router.navigate(['/listings', this.listing?.id]);
  }
}
