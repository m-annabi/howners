import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RentalService } from '../rental.service';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Rental, RentalStatus, RENTAL_STATUS_LABELS, RENTAL_STATUS_COLORS } from '../../../core/models/rental.model';
import { Contract } from '../../../core/models/contract.model';

@Component({
  selector: 'app-rental-detail',
  templateUrl: './rental-detail.component.html',
  styleUrls: ['./rental-detail.component.scss']
})
export class RentalDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  rental: Rental | null = null;
  contracts: Contract[] = [];
  loading = true;
  error: string | null = null;
  loadingContracts = false;
  creatingContract = false;
  showTemplateSelector = false;

  // Publish modal
  showPublishModal = false;
  publishTitle = '';
  publishDescription = '';
  publishAvailableFrom = '';
  publishLoading = false;

  // Exit tenant modal
  showExitModal = false;
  exitDate = '';
  exitNotes = '';
  exitLoading = false;

  // Confirm exit
  confirmExitLoading = false;

  RentalStatus = RentalStatus;
  rentalStatusLabels = RENTAL_STATUS_LABELS;
  rentalStatusColors = RENTAL_STATUS_COLORS;

  constructor(
    private rentalService: RentalService,
    private contractService: ContractService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRental(id);
      this.loadContracts(id);
    }
  }

  loadRental(id: string): void {
    this.loading = true;
    this.rentalService.getRental(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (rental) => {
        this.rental = rental;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement de la location';
        this.loading = false;
      }
    });
  }

  loadContracts(rentalId: string): void {
    this.loadingContracts = true;
    this.contractService.getContractsByRental(rentalId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.loadingContracts = false;
      },
      error: () => {
        this.loadingContracts = false;
      }
    });
  }

  // Publish modal
  openPublishModal(): void {
    this.publishTitle = '';
    this.publishDescription = '';
    this.publishAvailableFrom = '';
    this.showPublishModal = true;
  }

  closePublishModal(): void {
    this.showPublishModal = false;
  }

  submitPublish(): void {
    if (!this.rental || !this.publishTitle.trim()) return;
    this.publishLoading = true;
    this.rentalService.publishRental(this.rental.id, {
      title: this.publishTitle.trim(),
      description: this.publishDescription || undefined,
      availableFrom: this.publishAvailableFrom || undefined
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.publishLoading = false;
        this.showPublishModal = false;
        this.notificationService.success('Annonce publiée avec succès !');
        this.loadRental(this.rental!.id);
      },
      error: (err) => {
        this.publishLoading = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la publication');
      }
    });
  }

  // Exit tenant modal
  openExitModal(): void {
    this.exitDate = '';
    this.exitNotes = '';
    this.showExitModal = true;
  }

  closeExitModal(): void {
    this.showExitModal = false;
  }

  submitExitTenant(): void {
    if (!this.rental || !this.exitDate) return;
    this.exitLoading = true;
    this.rentalService.exitTenant(this.rental.id, {
      exitDate: this.exitDate,
      notes: this.exitNotes || undefined
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.exitLoading = false;
        this.showExitModal = false;
        this.notificationService.success('Sortie du locataire enregistrée.');
        this.loadRental(this.rental!.id);
      },
      error: (err) => {
        this.exitLoading = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la sortie du locataire');
      }
    });
  }

  // Contract
  openTemplateSelector(): void {
    if (!this.rental) return;
    this.showTemplateSelector = true;
  }

  closeTemplateSelector(): void {
    this.showTemplateSelector = false;
  }

  createContractWithTemplate(templateId: string | null): void {
    if (!this.rental) return;
    this.showTemplateSelector = false;
    this.creatingContract = true;
    this.contractService.createContract({ rentalId: this.rental.id, templateId }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (contract) => {
        this.creatingContract = false;
        this.notificationService.success('Contrat généré avec succès !');
        this.router.navigate(['/contracts', contract.id]);
      },
      error: (err) => {
        this.creatingContract = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la génération du contrat');
      }
    });
  }

  viewContract(contractId: string): void {
    this.router.navigate(['/contracts', contractId]);
  }

  hasContract(): boolean {
    return this.contracts.length > 0;
  }

  getMainContract(): Contract | null {
    return this.contracts.length > 0 ? this.contracts[0] : null;
  }

  editRental(): void {
    if (this.rental) {
      this.router.navigate(['/rentals', this.rental.id, 'edit']);
    }
  }

  deleteRental(): void {
    if (!this.rental) return;
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette location ?')) return;
    this.rentalService.deleteRental(this.rental.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.router.navigate(['/rentals']),
      error: (err) => this.notificationService.error(err.error?.message || 'Erreur lors de la suppression')
    });
  }

  confirmExit(): void {
    if (!this.rental || !confirm('Confirmer la sortie du locataire ?')) return;
    this.confirmExitLoading = true;
    this.rentalService.confirmExit(this.rental.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.confirmExitLoading = false;
        this.notificationService.success('Sortie confirmée.');
        this.loadRental(this.rental!.id);
      },
      error: (err) => {
        this.confirmExitLoading = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la confirmation');
      }
    });
  }

  viewPayments(): void { this.router.navigate(['/payments']); }
  viewInvoices(): void { this.router.navigate(['/invoices']); }
  viewStats(): void { this.router.navigate(['/financial']); }
  goBack(): void { this.router.navigate(['/rentals']); }

  getStatusColor(status: string): string {
    return this.rentalStatusColors[status as keyof typeof RENTAL_STATUS_COLORS];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
