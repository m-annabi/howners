import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RentalService } from '../rental.service';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Rental, RENTAL_TYPE_LABELS, RENTAL_STATUS_LABELS, RENTAL_STATUS_COLORS } from '../../../core/models/rental.model';
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

  rentalTypeLabels = RENTAL_TYPE_LABELS;
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
      error: (err) => {
        console.error('Erreur lors du chargement des contrats:', err);
        this.loadingContracts = false;
      }
    });
  }

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

    const request = {
      rentalId: this.rental.id,
      templateId: templateId  // null = template par défaut, sinon ID du template sélectionné
    };

    this.contractService.createContract(request).pipe(takeUntil(this.destroy$)).subscribe({
      next: (contract) => {
        this.creatingContract = false;
        this.notificationService.success('Contrat généré avec succès !');
        // Rediriger vers le contrat
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
    // Retourner le contrat le plus récent
    return this.contracts.length > 0 ? this.contracts[0] : null;
  }

  editRental(): void {
    if (this.rental) {
      this.router.navigate(['/rentals', this.rental.id, 'edit']);
    }
  }

  deleteRental(): void {
    if (!this.rental) return;

    if (!confirm('Êtes-vous sûr de vouloir supprimer cette location ?')) {
      return;
    }

    this.rentalService.deleteRental(this.rental.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.router.navigate(['/rentals']);
      },
      error: (err) => {
        this.notificationService.error(err.error?.message || 'Erreur lors de la suppression');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/rentals']);
  }

  getStatusColor(status: string): string {
    return this.rentalStatusColors[status as keyof typeof RENTAL_STATUS_COLORS];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
