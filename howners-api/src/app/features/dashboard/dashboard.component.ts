import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PaymentService } from '../../core/services/payment.service';
import { ContractService } from '../../core/services/contract.service';
import { ApplicationService } from '../../core/services/application.service';
import { User } from '../../core/models/user.model';
import { DashboardStats } from '../../core/models/dashboard.model';
import { PaymentStatus } from '../../core/models/payment.model';
import { ContractStatus } from '../../core/models/contract.model';
import { ApplicationStatus } from '../../core/models/application.model';

interface ActionItems {
  latePayments: number;
  expiringContracts: number;
  pendingApplications: number;
  awaitingSignatures: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private userSub!: Subscription;
  currentUser: User | null = null;
  stats: DashboardStats | null = null;
  loading = true;
  error: string | null = null;

  actionItems: ActionItems | null = null;

  get isTenant(): boolean {
    return this.currentUser?.role === 'TENANT';
  }

  get hasActionItems(): boolean {
    return !!this.actionItems && (
      this.actionItems.latePayments > 0 ||
      this.actionItems.expiringContracts > 0 ||
      this.actionItems.pendingApplications > 0 ||
      this.actionItems.awaitingSignatures > 0
    );
  }

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private paymentService: PaymentService,
    private contractService: ContractService,
    private applicationService: ApplicationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.loadStats();
      if (user && user.role !== 'TENANT') {
        this.loadActionItems();
      }
    });
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
  }

  get hasTenantAlerts(): boolean {
    return !!this.stats?.tenantInfo && (
      this.stats.tenantInfo.pendingInvitations > 0 ||
      this.stats.tenantInfo.pendingApplications > 0 ||
      !this.stats.tenantInfo.searchProfileActive
    );
  }

  get hasActivity(): boolean {
    return !!this.stats && (
      this.stats.pendingRentals > 0 ||
      !!this.stats.recentActivity?.latestProperty ||
      !!this.stats.recentActivity?.latestRental
    );
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;

    this.dashboardService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des statistiques';
        this.loading = false;
      }
    });
  }

  /**
   * For owners/admins: count urgent items across lists. Computed client-side from
   * existing endpoints — no new backend needed.
   */
  loadActionItems(): void {
    const today = new Date();
    const in30Days = new Date();
    in30Days.setDate(today.getDate() + 30);

    forkJoin({
      payments: this.paymentService.getAll().pipe(catchError(() => of([]))),
      contracts: this.contractService.getMyContracts().pipe(catchError(() => of([]))),
      applications: this.applicationService.getReceivedApplications().pipe(catchError(() => of([])))
    }).pipe(
      map(({ payments, contracts, applications }) => {
        const latePayments = payments.filter(p =>
          p.status === PaymentStatus.LATE ||
          (p.status === PaymentStatus.PENDING && p.dueDate && new Date(p.dueDate) < today)
        ).length;

        const expiringContracts = contracts.filter(c => {
          if (c.status !== ContractStatus.ACTIVE && c.status !== ContractStatus.SIGNED) return false;
          const end = (c as any).rentalEndDate || (c as any).endDate;
          if (!end) return false;
          const endDate = new Date(end);
          return endDate >= today && endDate <= in30Days;
        }).length;

        const awaitingSignatures = contracts.filter(c => c.status === ContractStatus.SENT).length;

        const pendingApplications = applications.filter(a =>
          a.status === ApplicationStatus.SUBMITTED ||
          a.status === ApplicationStatus.UNDER_REVIEW
        ).length;

        return { latePayments, expiringContracts, pendingApplications, awaitingSignatures };
      })
    ).subscribe(items => {
      this.actionItems = items;
    });
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  goToLatePayments(): void {
    this.router.navigate(['/payments'], { queryParams: { filter: 'late' } });
  }

  goToExpiringContracts(): void {
    this.router.navigate(['/contracts'], { queryParams: { filter: 'expiring' } });
  }

  goToAwaitingSignatures(): void {
    this.router.navigate(['/contracts'], { queryParams: { filter: 'sent' } });
  }

  goToPendingApplications(): void {
    this.router.navigate(['/applications'], { queryParams: { filter: 'pending' } });
  }
}
