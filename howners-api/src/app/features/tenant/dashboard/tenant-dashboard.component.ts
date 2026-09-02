import { downloadBlob } from '../../../shared/utils/file.utils';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { TenantApiService } from '../../../core/services/tenant-api.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { PaymentService } from '../../../core/services/payment.service';
import { ReceiptService } from '../../../core/services/receipt.service';
import { User } from '../../../core/models/user.model';
import { Rental, RentalStatus } from '../../../core/models/rental.model';
import { Payment, PaymentStatus } from '../../../core/models/payment.model';
import { Receipt } from '../../../core/models/receipt.model';
import { DashboardStats } from '../../../core/models/dashboard.model';
import { TenantContract } from '../../../core/services/tenant-api.service';
import { InAppNotificationService } from '../../../core/services/in-app-notification.service';
import { InAppNotification } from '../../../core/models/in-app-notification.model';

@Component({
  selector: 'app-tenant-dashboard',
  templateUrl: './tenant-dashboard.component.html',
  styleUrls: ['./tenant-dashboard.component.scss']
})
export class TenantDashboardComponent implements OnInit, OnDestroy {
  private userSub!: Subscription;

  currentUser: User | null = null;
  activeRental: Rental | null = null;
  activeContract: TenantContract | null = null;
  nextPayment: Payment | null = null;
  recentPayments: Payment[] = [];
  lastReceipt: Receipt | null = null;
  stats: DashboardStats | null = null;

  loading = true;
  error: string | null = null;
  downloadingReceipt = false;

  /** Alertes « à consulter » : documents/contrats reçus non lus, mis en avant sur le dashboard. */
  documentAlerts: InAppNotification[] = [];
  private notifSub?: Subscription;

  readonly RentalStatus = RentalStatus;
  readonly PaymentStatus = PaymentStatus;

  constructor(
    private authService: AuthService,
    private tenantApiService: TenantApiService,
    private dashboardService: DashboardService,
    private paymentService: PaymentService,
    private receiptService: ReceiptService,
    private router: Router,
    private inAppNotificationService: InAppNotificationService
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.loadData();

    // Alertes documents/contrats reçus : rafraîchir puis écouter les notifications.
    this.inAppNotificationService.loadNotifications();
    this.notifSub = this.inAppNotificationService.notifications$.subscribe(notifs => {
      this.documentAlerts = notifs.filter(n => !n.isRead && this.isDocumentAlert(n));
    });
  }

  /** Notification concernant un document/contrat/signature/état des lieux reçu. */
  private isDocumentAlert(n: InAppNotification): boolean {
    const t = (n.type || '').toLowerCase();
    return ['document', 'contract', 'contrat', 'signature', 'inventory', 'lease', 'edl']
      .some(k => t.includes(k));
  }

  /** Ouvre l'élément lié à l'alerte et la marque comme lue. */
  openAlert(n: InAppNotification): void {
    this.inAppNotificationService.markAsRead(n.id);
    this.documentAlerts = this.documentAlerts.filter(a => a.id !== n.id);
    if (n.route) {
      this.router.navigateByUrl(n.route);
    }
  }

  dismissAlert(n: InAppNotification, event: MouseEvent): void {
    event.stopPropagation();
    this.inAppNotificationService.markAsRead(n.id);
    this.documentAlerts = this.documentAlerts.filter(a => a.id !== n.id);
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.notifSub?.unsubscribe();
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      rentals: this.tenantApiService.getMyRentals().pipe(catchError(() => of([]))),
      contracts: this.tenantApiService.getMyContracts().pipe(catchError(() => of([]))),
      payments: this.paymentService.getAll().pipe(catchError(() => of([]))),
      receipts: this.receiptService.getAll().pipe(catchError(() => of([]))),
      stats: this.dashboardService.getStats().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ rentals, contracts, payments, receipts, stats }) => {
        this.stats = stats;

        this.activeRental = rentals.find(r =>
          r.status === RentalStatus.ACTIVE || r.status === RentalStatus.EXITING
        ) ?? rentals[0] ?? null;

        if (this.activeRental) {
          this.activeContract = contracts.find(c => c.rentalId === this.activeRental!.id) ?? null;
        }

        const pending = payments
          .filter(p => p.status === PaymentStatus.PENDING || p.status === PaymentStatus.LATE)
          .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime());
        this.nextPayment = pending[0] ?? null;

        this.recentPayments = payments
          .filter(p => p.id !== this.nextPayment?.id)
          .sort((a, b) => new Date(b.dueDate).getTime() - new Date(a.dueDate).getTime())
          .slice(0, 3);

        this.lastReceipt = [...receipts]
          .sort((a, b) => new Date(b.periodEnd).getTime() - new Date(a.periodEnd).getTime())[0] ?? null;

        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement';
        this.loading = false;
      }
    });
  }

  get unreadMessages(): number {
    return this.stats?.tenantInfo?.unreadMessages ?? 0;
  }

  get pendingApplications(): number {
    return this.stats?.tenantInfo?.pendingApplications ?? 0;
  }

  /** Loyer charges comprises — le montant réellement dû chaque mois. */
  get totalMonthly(): number {
    if (!this.activeRental) return 0;
    return this.activeRental.monthlyRent + (this.activeRental.charges ?? 0);
  }

  get isPaymentLate(): boolean {
    return this.nextPayment?.status === PaymentStatus.LATE ||
      (this.nextPayment?.status === PaymentStatus.PENDING &&
        !!this.nextPayment.dueDate &&
        new Date(this.nextPayment.dueDate) < new Date());
  }

  downloadLastReceipt(): void {
    if (!this.lastReceipt || this.downloadingReceipt) return;
    this.downloadingReceipt = true;
    this.receiptService.downloadPdf(this.lastReceipt.id).subscribe({
      next: blob => {
        downloadBlob(blob, `quittance-${this.lastReceipt!.receiptNumber}.pdf`);
        this.downloadingReceipt = false;
      },
      error: () => {
        this.downloadingReceipt = false;
      }
    });
  }

  paymentStatusLabel(status: PaymentStatus): string {
    switch (status) {
      case PaymentStatus.PAID: return 'Payé';
      case PaymentStatus.PENDING: return 'En attente';
      case PaymentStatus.LATE: return 'En retard';
      case PaymentStatus.FAILED: return 'Échoué';
      case PaymentStatus.REFUNDED: return 'Remboursé';
      case PaymentStatus.CANCELLED: return 'Annulé';
      default: return status;
    }
  }

  formatDate(date: string | undefined): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  formatDateShort(date: string | undefined): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatPeriod(receipt: Receipt): string {
    return new Date(receipt.periodStart).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  formatAmount(amount: number, currency = 'EUR'): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}
