export interface WidgetConfig {
  id: string;
  visible: boolean;
  order: number;
}

export type WidgetPage = 'dashboard' | 'financial';

export interface WidgetDef {
  id: string;
  label: string;
  icon: string;
  category: string;
  size: 'sm' | 'lg';
}

// Used by widget-configurator (financial dashboard)
export interface WidgetDefinition {
  id: string;
  label: string;
  icon: string;
}

export const ALL_WIDGET_DEFS: WidgetDef[] = [
  { id: 'stat-properties', label: 'Biens',             icon: 'bi-building',      category: 'Statistiques', size: 'sm' },
  { id: 'stat-rentals',    label: 'Locations actives',  icon: 'bi-key',           category: 'Statistiques', size: 'sm' },
  { id: 'stat-revenue',    label: 'Revenus mensuels',   icon: 'bi-cash-stack',    category: 'Statistiques', size: 'sm' },
  { id: 'stat-pending',    label: 'En attente',         icon: 'bi-clock-history', category: 'Statistiques', size: 'sm' },
  { id: 'action-items',    label: 'À traiter',          icon: 'bi-bell',          category: 'Gestion',      size: 'lg' },
  { id: 'recent-activity', label: 'Activité récente',   icon: 'bi-clock-history', category: 'Gestion',      size: 'lg' },
  { id: 'top-tenants',     label: 'Locataires potentiels', icon: 'bi-people',     category: 'Découverte',   size: 'lg' },
  { id: 'quick-actions',   label: 'Raccourcis',         icon: 'bi-lightning',     category: 'Navigation',   size: 'lg' },
];

export const FINANCIAL_WIDGET_DEFS: WidgetDefinition[] = [
  { id: 'financial-kpis',     label: 'Indicateurs clés',       icon: 'bi-graph-up' },
  { id: 'monthly-chart',      label: 'Évolution mensuelle',    icon: 'bi-bar-chart' },
  { id: 'expense-categories', label: 'Dépenses par catégorie', icon: 'bi-pie-chart' },
];
