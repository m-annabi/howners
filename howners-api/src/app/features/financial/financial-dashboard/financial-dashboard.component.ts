import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FinancialDashboardService } from '../../../core/services/financial-dashboard.service';
import { WidgetPreferenceService } from '../../../core/services/widget-preference.service';
import { FinancialDashboard } from '../../../core/models/financial-dashboard.model';
import { WidgetConfig, FINANCIAL_WIDGET_DEFS, WidgetDefinition } from '../../../core/models/widget-config.model';

@Component({
  selector: 'app-financial-dashboard',
  templateUrl: './financial-dashboard.component.html',
  styleUrls: ['./financial-dashboard.component.scss']
})
export class FinancialDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  dashboard: FinancialDashboard | null = null;
  loading = false;
  error: string | null = null;
  maxMonthlyValue = 0;

  // Widget system
  widgetConfigs: WidgetConfig[] = [];
  configuratorOpen = false;
  readonly WIDGET_DEFS: WidgetDefinition[] = FINANCIAL_WIDGET_DEFS;

  get sortedVisibleWidgets(): WidgetConfig[] {
    return this.widgetConfigs.filter(w => w.visible).sort((a, b) => a.order - b.order);
  }

  constructor(
    private financialService: FinancialDashboardService,
    private widgetPreferenceService: WidgetPreferenceService
  ) {}

  ngOnInit(): void {
    this.loadWidgetPreferences();
    this.loadDashboard();
  }

  loadWidgetPreferences(): void {
    this.widgetPreferenceService.getPreferences('financial').pipe(takeUntil(this.destroy$)).subscribe({
      next: configs => { this.widgetConfigs = configs; },
      error: () => {
        this.widgetConfigs = FINANCIAL_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
      }
    });
  }

  onSaveWidgets(configs: WidgetConfig[]): void {
    this.widgetConfigs = configs;
    this.configuratorOpen = false;
    this.widgetPreferenceService.savePreferences('financial', configs).pipe(takeUntil(this.destroy$)).subscribe();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;
    this.financialService.getDashboard().pipe(takeUntil(this.destroy$)).subscribe({
      next: dashboard => {
        this.dashboard = dashboard;
        this.maxMonthlyValue = Math.max(
          ...dashboard.monthlyBreakdown.map(m => Math.max(m.revenue, m.expenses)),
          1
        );
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement du tableau de bord financier';
        this.loading = false;
      }
    });
  }

  getBarHeight(value: number): string {
    return Math.max((value / this.maxMonthlyValue) * 100, 2) + '%';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
