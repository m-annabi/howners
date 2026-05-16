import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PaymentService } from '../../core/services/payment.service';
import { ContractService } from '../../core/services/contract.service';
import { ApplicationService } from '../../core/services/application.service';
import { TenantDiscoveryService } from '../../core/services/tenant-discovery.service';
import { WidgetPreferenceService } from '../../core/services/widget-preference.service';
import { User } from '../../core/models/user.model';
import { DashboardStats } from '../../core/models/dashboard.model';
import { PaymentStatus } from '../../core/models/payment.model';
import { ContractStatus } from '../../core/models/contract.model';
import { ApplicationStatus } from '../../core/models/application.model';
import { TenantSearchResult } from '../../core/models/tenant-search-result.model';
import {
  WidgetConfig, DASHBOARD_WIDGET_DEFS,
  ShortcutDef, OverviewStatDef,
  ALL_SHORTCUTS, ALL_OVERVIEW_STATS,
  DEFAULT_SHORTCUT_IDS, DEFAULT_OVERVIEW_STAT_IDS
} from '../../core/models/widget-config.model';

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
  topTenants: TenantSearchResult[] = [];

  widgetConfigs: WidgetConfig[] = [];
  editMode = false;
  editConfigs: WidgetConfig[] = [];

  readonly ALL_SHORTCUTS: ShortcutDef[] = ALL_SHORTCUTS;
  readonly ALL_OVERVIEW_STATS: OverviewStatDef[] = ALL_OVERVIEW_STATS;

  get isTenant(): boolean { return this.currentUser?.role === 'TENANT'; }

  get sortedVisibleWidgets(): WidgetConfig[] {
    return this.widgetConfigs.filter(w => w.visible).sort((a, b) => a.order - b.order);
  }

  get hasActionItems(): boolean {
    return !!this.actionItems && (
      this.actionItems.latePayments > 0 ||
      this.actionItems.expiringContracts > 0 ||
      this.actionItems.pendingApplications > 0 ||
      this.actionItems.awaitingSignatures > 0
    );
  }

  get showOnboarding(): boolean {
    return !this.isTenant && !this.loading && !!this.stats && (this.stats.totalProperties || 0) === 0;
  }

  get onboardingStepProperty(): boolean { return !!this.stats && (this.stats.totalProperties || 0) > 0; }
  get onboardingStepRental(): boolean { return !!this.stats && (this.stats.activeRentals || 0) > 0; }
  get onboardingStepContract(): boolean { return this.onboardingStepRental; }

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

  get visibleOverviewStats(): OverviewStatDef[] {
    const ids = this.getVisibleItems('overview');
    return ALL_OVERVIEW_STATS.filter(s => ids.includes(s.id));
  }

  get visibleShortcuts(): ShortcutDef[] {
    const ids = this.getVisibleItems('quick-actions');
    return ALL_SHORTCUTS.filter(s => ids.includes(s.id));
  }

  getVisibleItems(widgetId: string): string[] {
    const cfg = this.widgetConfigs.find(w => w.id === widgetId);
    if (!cfg?.items || cfg.items.length === 0) {
      if (widgetId === 'quick-actions') return DEFAULT_SHORTCUT_IDS;
      if (widgetId === 'overview') return DEFAULT_OVERVIEW_STAT_IDS;
      return [];
    }
    return cfg.items;
  }

  getStatValue(statId: string): string | number {
    if (!this.stats) return '-';
    switch (statId) {
      case 'properties': return this.stats.totalProperties ?? 0;
      case 'rentals':    return this.stats.activeRentals ?? 0;
      case 'revenue':    return `${this.stats.monthlyRevenue || 0} ${this.stats.currency || 'EUR'}`;
      case 'pending':    return this.stats.pendingRentals ?? 0;
      default:           return '-';
    }
  }

  getWidgetLabel(id: string): string {
    return DASHBOARD_WIDGET_DEFS.find(d => d.id === id)?.label ?? id;
  }

  getWidgetIcon(id: string): string {
    return DASHBOARD_WIDGET_DEFS.find(d => d.id === id)?.icon ?? 'bi-grid';
  }

  // ── Edit mode ──────────────────────────────────────────────────────────

  enterEditMode(): void {
    this.editConfigs = this.widgetConfigs
      .map(w => ({ ...w, items: w.items ? [...w.items] : undefined }))
      .sort((a, b) => a.order - b.order);
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
    this.editConfigs = [];
  }

  saveEdit(): void {
    this.editConfigs.forEach((w, i) => w.order = i);
    this.widgetConfigs = this.editConfigs.map(w => ({ ...w }));
    this.editMode = false;
    this.editConfigs = [];
    this.widgetPreferenceService.savePreferences('dashboard', this.widgetConfigs).subscribe();
  }

  toggleWidgetVisible(id: string): void {
    const cfg = this.editConfigs.find(w => w.id === id);
    if (cfg) cfg.visible = !cfg.visible;
  }

  isItemVisibleInEdit(widgetId: string, itemId: string): boolean {
    const cfg = this.editConfigs.find(w => w.id === widgetId);
    const items = cfg?.items;
    if (!items || items.length === 0) {
      if (widgetId === 'quick-actions') return DEFAULT_SHORTCUT_IDS.includes(itemId);
      if (widgetId === 'overview') return DEFAULT_OVERVIEW_STAT_IDS.includes(itemId);
      return false;
    }
    return items.includes(itemId);
  }

  toggleItem(widgetId: string, itemId: string): void {
    const cfg = this.editConfigs.find(w => w.id === widgetId);
    if (!cfg) return;
    if (!cfg.items || cfg.items.length === 0) {
      cfg.items = widgetId === 'quick-actions' ? [...DEFAULT_SHORTCUT_IDS]
                : widgetId === 'overview'       ? [...DEFAULT_OVERVIEW_STAT_IDS]
                : [];
    }
    const idx = cfg.items.indexOf(itemId);
    if (idx >= 0) cfg.items.splice(idx, 1);
    else cfg.items.push(itemId);
  }

  onWidgetDrop(event: CdkDragDrop<WidgetConfig[]>): void {
    moveItemInArray(this.editConfigs, event.previousIndex, event.currentIndex);
  }

  // ── Lifecycle & data loading ──────────────────────────────────────────

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private paymentService: PaymentService,
    private contractService: ContractService,
    private applicationService: ApplicationService,
    private tenantDiscoveryService: TenantDiscoveryService,
    private widgetPreferenceService: WidgetPreferenceService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.loadStats();
      if (user && user.role !== 'TENANT') {
        this.loadWidgetPreferences();
        this.loadActionItems();
        this.loadTopTenants();
      }
    });
  }

  ngOnDestroy(): void { this.userSub?.unsubscribe(); }

  loadStats(): void {
    this.loading = true;
    this.error = null;
    this.dashboardService.getStats().subscribe({
      next: stats => { this.stats = stats; this.loading = false; },
      error: err => {
        this.error = err.error?.message || 'Erreur lors du chargement des statistiques';
        this.loading = false;
      }
    });
  }

  loadWidgetPreferences(): void {
    this.widgetPreferenceService.getPreferences('dashboard').subscribe({
      next: configs => { this.widgetConfigs = configs; },
      error: () => {
        this.widgetConfigs = DASHBOARD_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
      }
    });
  }

  loadTopTenants(): void {
    this.tenantDiscoveryService.searchTenants({ sortBy: 'score' }).subscribe({
      next: results => { this.topTenants = (results || []).slice(0, 3); },
      error: () => {}
    });
  }

  loadActionItems(): void {
    const today = new Date();
    const in30Days = new Date();
    in30Days.setDate(today.getDate() + 30);

    forkJoin({
      payments: this.paymentService.getAll().pipe(catchError(() => of([]))),
      contracts: this.contractService.getMyContracts().pipe(catchError(() => of([]))),
      applications: this.applicationService.getReceivedApplications().pipe(catchError(() => of([])))
    }).pipe(
      map(({ payments, contracts, applications }) => ({
        latePayments: payments.filter(p =>
          p.status === PaymentStatus.LATE ||
          (p.status === PaymentStatus.PENDING && p.dueDate && new Date(p.dueDate) < today)
        ).length,
        expiringContracts: contracts.filter(c => {
          if (c.status !== ContractStatus.ACTIVE && c.status !== ContractStatus.SIGNED) return false;
          const end = (c as any).rentalEndDate || (c as any).endDate;
          if (!end) return false;
          const d = new Date(end);
          return d >= today && d <= in30Days;
        }).length,
        awaitingSignatures: contracts.filter(c => c.status === ContractStatus.SENT).length,
        pendingApplications: applications.filter(a =>
          a.status === ApplicationStatus.SUBMITTED || a.status === ApplicationStatus.UNDER_REVIEW
        ).length,
      }))
    ).subscribe(items => { this.actionItems = items; });
  }

  navigateTo(path: string): void { this.router.navigate([path]); }
  goToLatePayments(): void { this.router.navigate(['/payments'], { queryParams: { filter: 'late' } }); }
  goToExpiringContracts(): void { this.router.navigate(['/contracts'], { queryParams: { filter: 'expiring' } }); }
  goToAwaitingSignatures(): void { this.router.navigate(['/contracts'], { queryParams: { filter: 'sent' } }); }
  goToPendingApplications(): void { this.router.navigate(['/applications'], { queryParams: { filter: 'pending' } }); }
  goToTenantDiscovery(): void { this.router.navigate(['/tenant-discovery']); }
}
