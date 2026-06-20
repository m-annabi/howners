import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { TenantApiService } from '../../../core/services/tenant-api.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { PaymentService } from '../../../core/services/payment.service';
import { User } from '../../../core/models/user.model';
import { Rental, RentalStatus } from '../../../core/models/rental.model';
import { Payment, PaymentStatus } from '../../../core/models/payment.model';
import { DashboardStats } from '../../../core/models/dashboard.model';
import { TenantContract } from '../../../core/services/tenant-api.service';

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
  stats: DashboardStats | null = null;

  loading = true;
  error: string | null = null;

  readonly RentalStatus = RentalStatus;
  readonly PaymentStatus = PaymentStatus;

  constructor(
    private authService: AuthService,
    private tenantApiService: TenantApiService,
    private dashboardService: DashboardService,
    private paymentService: PaymentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.loadData();
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  private loadData(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      rentals: this.tenantApiService.getMyRentals().pipe(catchError(() => of([]))),
      contracts: this.tenantApiService.getMyContracts().pipe(catchError(() => of([]))),
      payments: this.paymentService.getAll().pipe(catchError(() => of([]))),
      stats: this.dashboardService.getStats().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ rentals, contracts, payments, stats }) => {
        this.stats = stats;

        this.activeRental = rentals.find(r =>
          r.status === RentalStatus.ACTIVE || r.status === RentalStatus.EXITING
        ) ?? rentals[0] ?? null;

        if (this.activeRental) {
          this.activeContract = contracts.find(c => c.rentalId === this.activeRental!.id) ?? null;
        }

        const today = new Date();
        const pending = payments
          .filter(p => p.status === PaymentStatus.PENDING || p.status === PaymentStatus.LATE)
          .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime());
        this.nextPayment = pending[0] ?? null;

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

  get isPaymentLate(): boolean {
    return this.nextPayment?.status === PaymentStatus.LATE ||
      (this.nextPayment?.status === PaymentStatus.PENDING &&
        !!this.nextPayment.dueDate &&
        new Date(this.nextPayment.dueDate) < new Date());
  }

  formatDate(date: string | undefined): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  formatAmount(amount: number, currency = 'EUR'): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}
