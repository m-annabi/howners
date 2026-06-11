import { Component, OnInit, OnDestroy, HostListener, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import Sortable from 'sortablejs';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PaymentService } from '../../core/services/payment.service';
import { ContractService } from '../../core/services/contract.service';
import { ApplicationService } from '../../core/services/application.service';
import { TenantDiscoveryService } from '../../core/services/tenant-discovery.service';
import { WidgetPreferenceService } from '../../core/services/widget-preference.service';
import { OnboardingService, OnboardingStatus } from '../../core/services/onboarding.service';
import { AnalyticsService, AnalyticsSummary } from '../../core/services/analytics.service';
import { ExportService } from '../../core/services/export.service';
import { User } from '../../core/models/user.model';
import { DashboardStats } from '../../core/models/dashboard.model';
import { PaymentStatus } from '../../core/models/payment.model';
import { ContractStatus } from '../../core/models/contract.model';
import { ApplicationStatus } from '../../core/models/application.model';
import { TenantSearchResult } from '../../core/models/tenant-search-result.model';
import { WidgetConfig, WidgetDef, ALL_WIDGET_DEFS } from '../../core/models/widget-config.model';

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
  private sortable?: Sortable;

  currentUser: User | null = null;
  stats: DashboardStats | null = null;
  loading = true;
  error: string | null = null;
  actionItems: ActionItems | null = null;
  topTenants: TenantSearchResult[] = [];

  showChecklist = false;

  // Analytics
  analyticsSummary: AnalyticsSummary | null = null;
  analyticsLoading = false;
  maxMonthlyRevenue = 0;

  widgetConfigs: WidgetConfig[] = [];
  displayWidgets: WidgetConfig[] = [];
  editWidgets: WidgetConfig[] = [];
  editMode = false;
  addPanelOpen = false;

  readonly ALL_WIDGET_DEFS: WidgetDef[] = ALL_WIDGET_DEFS;

  private _gridEl?: ElementRef<HTMLElement>;

  @ViewChild('widgetGrid', { static: false })
  set gridEl(el: ElementRef<HTMLElement> | undefined) {
    this._gridEl = el;
  }

  // ── Computed ───────────────────────────────────────────────────────────

  get isTenant(): boolean { return this.currentUser?.role === 'TENANT'; }
  get isOwner(): boolean { return this.currentUser?.role === 'OWNER'; }

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

  get inactiveWidgetDefs(): WidgetDef[] {
    const activeIds = this.displayWidgets.map(w => w.id);
    return ALL_WIDGET_DEFS.filter(d => !activeIds.includes(d.id));
  }

  get catalogGroups(): { category: string; defs: WidgetDef[] }[] {
    const inactive = this.inactiveWidgetDefs;
    const categories = [...new Set(inactive.map(d => d.category))];
    return categories.map(cat => ({
      category: cat,
      defs: inactive.filter(d => d.category === cat)
    }));
  }

  getWidgetSizeClass(id: string): string {
    const def = ALL_WIDGET_DEFS.find(d => d.id === id);
    return def?.size === 'sm' ? 'widget-sm' : 'widget-lg';
  }

  getWidgetDef(id: string): WidgetDef | undefined {
    return ALL_WIDGET_DEFS.find(d => d.id === id);
  }

  trackById(_: number, cfg: WidgetConfig): string { return cfg.id; }

  // ── Edit mode ──────────────────────────────────────────────────────────

  enterEditMode(): void {
    this.editWidgets = this.widgetConfigs.map(w => ({ ...w }));
    this.displayWidgets = [...this.editWidgets]
      .filter(w => w.visible)
      .sort((a, b) => a.order - b.order);
    this.editMode = true;
    setTimeout(() => this.initSortable());
  }

  cancelEdit(): void {
    this.editMode = false;
    this.addPanelOpen = false;
    this.destroySortable();
    this.refreshDisplayWidgets();
  }

  saveEdit(): void {
    this.displayWidgets.forEach((w, i) => w.order = i);
    const hiddenWidgets = this.editWidgets.filter(w => !w.visible);
    const maxOrder = this.displayWidgets.length;
    hiddenWidgets.forEach((w, i) => w.order = maxOrder + i);
    this.widgetConfigs = [...this.editWidgets];
    this.widgetPreferenceService.savePreferences('dashboard', this.widgetConfigs).subscribe();
    this.editMode = false;
    this.addPanelOpen = false;
    this.destroySortable();
  }

  removeWidget(id: string): void {
    const cfg = this.editWidgets.find(w => w.id === id);
    if (cfg) {
      cfg.visible = false;
      this.displayWidgets = this.editWidgets
        .filter(w => w.visible)
        .sort((a, b) => a.order - b.order);
      setTimeout(() => this.initSortable());
    }
  }

  addWidget(id: string): void {
    let cfg = this.editWidgets.find(w => w.id === id);
    if (cfg) {
      cfg.visible = true;
      cfg.order = this.displayWidgets.length;
    } else {
      cfg = { id, visible: true, order: this.displayWidgets.length };
      this.editWidgets.push(cfg);
    }
    this.displayWidgets = this.editWidgets
      .filter(w => w.visible)
      .sort((a, b) => a.order - b.order);
    this.addPanelOpen = false;
    setTimeout(() => this.initSortable());
  }

  private initSortable(): void {
    this.destroySortable();
    if (!this._gridEl) return;
    this.sortable = Sortable.create(this._gridEl.nativeElement, {
      animation: 150,
      ghostClass: 'widget-sortable-ghost',
      chosenClass: 'widget-sortable-chosen',
      filter: '.widget-remove',
      preventOnFilter: true,
      onEnd: (evt: Sortable.SortableEvent) => {
        const prev = evt.oldIndex!;
        const next = evt.newIndex!;
        if (prev !== next) {
          const moved = this.displayWidgets.splice(prev, 1)[0];
          this.displayWidgets.splice(next, 0, moved);
          this.displayWidgets.forEach((w, i) => w.order = i);
        }
      }
    });
  }

  private destroySortable(): void {
    this.sortable?.destroy();
    this.sortable = undefined;
  }

  private refreshDisplayWidgets(): void {
    this.displayWidgets = [...this.widgetConfigs]
      .filter(w => w.visible)
      .sort((a, b) => a.order - b.order);
  }

  private normalizeConfigs(configs: WidgetConfig[]): WidgetConfig[] {
    const knownIds = ALL_WIDGET_DEFS.map(d => d.id);
    const hasUnknown = configs.some(c => !knownIds.includes(c.id));
    if (hasUnknown || configs.length === 0) {
      return ALL_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
    }
    const savedIds = configs.map(c => c.id);
    const newDefs = ALL_WIDGET_DEFS.filter(d => !savedIds.includes(d.id));
    const maxOrder = Math.max(...configs.map(c => c.order), -1);
    return [
      ...configs,
      ...newDefs.map((d, i) => ({ id: d.id, visible: false, order: maxOrder + 1 + i }))
    ];
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!(event.target as HTMLElement).closest('.widget-add-wrapper')) {
      this.addPanelOpen = false;
    }
  }

  // ── Lifecycle & data ───────────────────────────────────────────────────

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private paymentService: PaymentService,
    private contractService: ContractService,
    private applicationService: ApplicationService,
    private tenantDiscoveryService: TenantDiscoveryService,
    private widgetPreferenceService: WidgetPreferenceService,
    private analyticsServiceFe: AnalyticsService,
    private exportService: ExportService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.loadStats();
      if (user && user.role !== 'TENANT') {
        this.showChecklist = true;
        this.loadWidgetPreferences();
        this.loadActionItems();
        this.loadTopTenants();
        if (user.role === 'OWNER') {
          this.loadAnalytics();
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.destroySortable();
    this.userSub?.unsubscribe();
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;
    this.dashboardService.getStats().subscribe({
      next: stats => { this.stats = stats; this.loading = false; },
      error: err => {
        this.error = err.error?.message || 'Erreur lors du chargement';
        this.loading = false;
      }
    });
  }

  loadWidgetPreferences(): void {
    this.widgetPreferenceService.getPreferences('dashboard').subscribe({
      next: configs => {
        this.widgetConfigs = this.normalizeConfigs(configs);
        this.refreshDisplayWidgets();
      },
      error: () => {
        this.widgetConfigs = ALL_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
        this.refreshDisplayWidgets();
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

  onChecklistHidden(): void { this.showChecklist = false; }

  loadAnalytics(): void {
    this.analyticsLoading = true;
    this.analyticsServiceFe.getSummary().subscribe({
      next: summary => {
        this.analyticsSummary = summary;
        this.maxMonthlyRevenue = Math.max(
          ...summary.monthlyRevenue.map(m => m.amount),
          1
        );
        this.analyticsLoading = false;
      },
      error: () => { this.analyticsLoading = false; }
    });
  }

  exportCsv(): void {
    const year = new Date().getFullYear();
    this.exportService.downloadFinancialCsv(year);
  }

  formatMonth(month: string): string {
    const [y, m] = month.split('-');
    const months = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec'];
    return months[parseInt(m, 10) - 1] || m;
  }

  getBarHeight(amount: number): number {
    if (this.maxMonthlyRevenue <= 0) return 0;
    return Math.round((amount / this.maxMonthlyRevenue) * 100);
  }

  navigateTo(path: string): void { this.router.navigate([path]); }
  goToLatePayments(): void { this.router.navigate(['/payments'], { queryParams: { filter: 'late' } }); }
  goToExpiringContracts(): void { this.router.navigate(['/contracts'], { queryParams: { filter: 'expiring' } }); }
  goToAwaitingSignatures(): void { this.router.navigate(['/contracts'], { queryParams: { filter: 'sent' } }); }
  goToPendingApplications(): void { this.router.navigate(['/applications'], { queryParams: { filter: 'pending' } }); }
  goToTenantDiscovery(): void { this.router.navigate(['/tenant-discovery']); }
}
