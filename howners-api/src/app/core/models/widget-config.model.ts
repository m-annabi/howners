export interface WidgetConfig {
  id: string;
  visible: boolean;
  order: number;
}

export type WidgetPage = 'dashboard' | 'financial';

export interface WidgetDefinition {
  id: string;
  label: string;
  icon: string;
}

export const DASHBOARD_WIDGET_DEFS: WidgetDefinition[] = [
  { id: 'overview',         label: 'Vue d\'ensemble',       icon: 'bi-grid-1x2' },
  { id: 'action-items',     label: 'À traiter',             icon: 'bi-exclamation-circle' },
  { id: 'top-tenants',      label: 'Locataires potentiels', icon: 'bi-people' },
  { id: 'recent-activity',  label: 'Activité récente',      icon: 'bi-clock-history' },
  { id: 'quick-actions',    label: 'Raccourcis',            icon: 'bi-grid' },
];

export const FINANCIAL_WIDGET_DEFS: WidgetDefinition[] = [
  { id: 'financial-kpis',       label: 'Indicateurs clés',        icon: 'bi-graph-up' },
  { id: 'monthly-chart',        label: 'Évolution mensuelle',      icon: 'bi-bar-chart' },
  { id: 'expense-categories',   label: 'Dépenses par catégorie',   icon: 'bi-pie-chart' },
];
