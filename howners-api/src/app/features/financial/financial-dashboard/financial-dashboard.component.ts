import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { FinancialDashboardService } from '../../../core/services/financial-dashboard.service';
import { WidgetPreferenceService } from '../../../core/services/widget-preference.service';
import { FinancialDashboard } from '../../../core/models/financial-dashboard.model';
import { WidgetConfig, WidgetDef, FINANCIAL_WIDGET_DEFS } from '../../../core/models/widget-config.model';

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

  widgetConfigs: WidgetConfig[] = [];
  displayWidgets: WidgetConfig[] = [];
  editWidgets: WidgetConfig[] = [];
  editMode = false;
  addPanelOpen = false;

  readonly ALL_WIDGET_DEFS: WidgetDef[] = FINANCIAL_WIDGET_DEFS;

  get inactiveWidgetDefs(): WidgetDef[] {
    const activeIds = this.displayWidgets.map(w => w.id);
    return FINANCIAL_WIDGET_DEFS.filter(d => !activeIds.includes(d.id));
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
    const def = FINANCIAL_WIDGET_DEFS.find(d => d.id === id);
    return def?.size === 'sm' ? 'widget-sm' : 'widget-lg';
  }

  getWidgetDef(id: string): WidgetDef | undefined {
    return FINANCIAL_WIDGET_DEFS.find(d => d.id === id);
  }

  enterEditMode(): void {
    this.editWidgets = this.widgetConfigs.map(w => ({ ...w }));
    this.displayWidgets = [...this.editWidgets]
      .filter(w => w.visible)
      .sort((a, b) => a.order - b.order);
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
    this.addPanelOpen = false;
    this.refreshDisplayWidgets();
  }

  saveEdit(): void {
    this.displayWidgets.forEach((w, i) => w.order = i);
    const hiddenWidgets = this.editWidgets.filter(w => !w.visible);
    const maxOrder = this.displayWidgets.length;
    hiddenWidgets.forEach((w, i) => w.order = maxOrder + i);
    this.widgetConfigs = [...this.editWidgets];
    this.widgetPreferenceService.savePreferences('financial', this.widgetConfigs)
      .pipe(takeUntil(this.destroy$)).subscribe();
    this.editMode = false;
    this.addPanelOpen = false;
  }

  removeWidget(id: string): void {
    const cfg = this.editWidgets.find(w => w.id === id);
    if (cfg) {
      cfg.visible = false;
      this.displayWidgets = this.editWidgets
        .filter(w => w.visible)
        .sort((a, b) => a.order - b.order);
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
  }

  onDrop(event: CdkDragDrop<WidgetConfig[]>): void {
    moveItemInArray(this.displayWidgets, event.previousIndex, event.currentIndex);
    this.displayWidgets.forEach((w, i) => w.order = i);
  }

  private refreshDisplayWidgets(): void {
    this.displayWidgets = [...this.widgetConfigs]
      .filter(w => w.visible)
      .sort((a, b) => a.order - b.order);
  }

  private normalizeConfigs(configs: WidgetConfig[]): WidgetConfig[] {
    const knownIds = FINANCIAL_WIDGET_DEFS.map(d => d.id);
    const hasUnknown = configs.some(c => !knownIds.includes(c.id));
    if (hasUnknown || configs.length === 0) {
      return FINANCIAL_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
    }
    const savedIds = configs.map(c => c.id);
    const newDefs = FINANCIAL_WIDGET_DEFS.filter(d => !savedIds.includes(d.id));
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
      next: configs => {
        this.widgetConfigs = this.normalizeConfigs(configs);
        this.refreshDisplayWidgets();
      },
      error: () => {
        this.widgetConfigs = FINANCIAL_WIDGET_DEFS.map((d, i) => ({ id: d.id, visible: true, order: i }));
        this.refreshDisplayWidgets();
      }
    });
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
