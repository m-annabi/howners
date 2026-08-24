import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RentalService } from '../rental.service';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Rental, RentalStatus, RENTAL_STATUS_LABELS, RENTAL_STATUS_COLORS } from '../../../core/models/rental.model';
import { Contract, ContractStatus } from '../../../core/models/contract.model';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import { EtatDesLieux, EtatDesLieuxType, EDL_TYPE_LABELS } from '../../../core/models/etat-des-lieux.model';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment, PaymentStatus } from '../../../core/models/payment.model';
import { StepItem } from '../../../shared/components/stepper/stepper.component';

@Component({
  selector: 'app-rental-detail',
  templateUrl: './rental-detail.component.html',
  styleUrls: ['./rental-detail.component.scss']
})
export class RentalDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  rental: Rental | null = null;
  contracts: Contract[] = [];
  edls: EtatDesLieux[] = [];
  payments: Payment[] = [];
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
  ContractStatus = ContractStatus;
  rentalStatusLabels = RENTAL_STATUS_LABELS;
  rentalStatusColors = RENTAL_STATUS_COLORS;
  edlTypeLabels = EDL_TYPE_LABELS;
  PaymentStatus = PaymentStatus;
  isOwner = false;

  constructor(
    private rentalService: RentalService,
    private contractService: ContractService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private authService: AuthService,
    private edlService: EtatDesLieuxService,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.isOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRental(id);
      this.loadContracts(id);
      this.loadEdls(id);
      this.loadPayments(id);
    }
  }

  loadEdls(rentalId: string): void {
    this.edlService.getByRental(rentalId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (edls) => this.edls = edls || [],
      error: () => this.edls = []
    });
  }

  loadPayments(rentalId: string): void {
    this.paymentService.getByRental(rentalId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (payments) => this.payments = payments || [],
      error: () => this.payments = []
    });
  }

  /**
   * Étapes d'installation du locataire, calculées à partir de l'état réel du bail
   * (bail créé → contrat généré → signé → état des lieux d'entrée → bail actif).
   * La première étape non terminée est « current », les précédentes « done ».
   */
  get onboardingSteps(): StepItem[] {
    const hasContract = this.contracts.length > 0;
    const signed = this.contracts.some(c => c.status === ContractStatus.SIGNED);
    const edlEntree = this.edls.some(e => e.type === EtatDesLieuxType.ENTREE);
    const active = this.rental?.status === RentalStatus.ACTIVE;

    const defs = [
      { label: 'Candidature acceptée', done: true },
      { label: 'Bail créé', done: !!this.rental },
      { label: 'Contrat généré', done: hasContract, hint: hasContract ? undefined : 'Générez le contrat de bail' },
      { label: 'Contrat signé', done: signed, hint: signed ? undefined : 'Envoyez le contrat à la signature' },
      { label: 'État des lieux d\'entrée', done: edlEntree, hint: edlEntree ? undefined : 'Réalisez l\'état des lieux d\'entrée' },
      { label: 'Bail actif', done: active }
    ];

    const firstTodo = defs.findIndex(d => !d.done);
    return defs.map((d, i) => ({
      label: d.label,
      hint: d.hint,
      state: d.done ? 'done' : (i === firstTodo ? 'current' : 'todo')
    }));
  }

  /** Le bail est-il encore en cours d'installation (stepper pertinent) ? */
  get showOnboarding(): boolean {
    const s = this.rental?.status;
    return !!s && s !== RentalStatus.TERMINATED && s !== RentalStatus.CANCELLED
        && s !== RentalStatus.VACANT && s !== RentalStatus.LISTED;
  }

  /** Le contrat de bail a-t-il été signé ? (signé ou bail devenu actif) */
  get contractSigned(): boolean {
    return this.contracts.some(c => c.status === ContractStatus.SIGNED || c.status === ContractStatus.ACTIVE)
        || this.rental?.status === RentalStatus.ACTIVE
        || this.rental?.status === RentalStatus.EXITING
        || this.rental?.status === RentalStatus.TERMINATED;
  }

  /**
   * Côté locataire, la location ne « existe » réellement qu'une fois le contrat
   * signé. Tant qu'il ne l'est pas, on présente un état « à finaliser » plutôt
   * qu'un logement actif (et on masque les statuts propriétaire type « Libre »).
   */
  get tenantLocationReady(): boolean {
    return this.contractSigned;
  }

  /** Libellé de statut adapté au locataire (pas de « Libre » / « En annonce »). */
  get tenantStatusLabel(): string {
    switch (this.rental?.status) {
      case RentalStatus.ACTIVE:     return 'Location active';
      case RentalStatus.EXITING:    return 'Sortie programmée';
      case RentalStatus.TERMINATED: return 'Location terminée';
      default:                      return 'En cours de finalisation';
    }
  }

  /** Prochaine échéance à régler (la plus proche parmi en attente / en retard). */
  get nextPayment(): Payment | null {
    return [...this.payments]
      .filter(p => p.status === PaymentStatus.PENDING || p.status === PaymentStatus.LATE)
      .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())[0] ?? null;
  }

  /** Derniers paiements (hors prochaine échéance), du plus récent au plus ancien. */
  get recentPayments(): Payment[] {
    const nextId = this.nextPayment?.id;
    return [...this.payments]
      .filter(p => p.id !== nextId)
      .sort((a, b) => new Date(b.dueDate).getTime() - new Date(a.dueDate).getTime())
      .slice(0, 4);
  }

  get isPaymentLate(): boolean {
    const p = this.nextPayment;
    return !!p && (p.status === PaymentStatus.LATE
      || (p.status === PaymentStatus.PENDING && !!p.dueDate && new Date(p.dueDate) < new Date()));
  }

  /** État des lieux d'entrée du bail, s'il existe. */
  get edlEntree(): EtatDesLieux | null {
    return this.edls.find(e => e.type === EtatDesLieuxType.ENTREE) ?? null;
  }

  paymentStatusLabel(status: PaymentStatus): string {
    switch (status) {
      case PaymentStatus.PAID:      return 'Payé';
      case PaymentStatus.PENDING:   return 'En attente';
      case PaymentStatus.LATE:      return 'En retard';
      case PaymentStatus.FAILED:    return 'Échoué';
      case PaymentStatus.REFUNDED:  return 'Remboursé';
      case PaymentStatus.CANCELLED: return 'Annulé';
      default:                      return status;
    }
  }

  formatAmount(amount: number, currency = 'EUR'): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
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

  /** Vers le contrat de bail principal, sinon vers la liste des contrats. */
  goToContract(): void {
    const main = this.getMainContract();
    this.router.navigate(main ? ['/contracts', main.id] : ['/contracts']);
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
  goBack(): void {
    // /rentals redirige le locataire vers ce détail : retour vers Mon espace
    if (this.authService.hasRole('TENANT')) {
      this.router.navigate(['/tenant/dashboard']);
    } else {
      this.router.navigate(['/rentals']);
    }
  }

  getStatusColor(status: string): string {
    return this.rentalStatusColors[status as keyof typeof RENTAL_STATUS_COLORS];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
