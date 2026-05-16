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
  route?: string;
  color?: string;
}

// Used by widget-configurator (financial dashboard)
export interface WidgetDefinition {
  id: string;
  label: string;
  icon: string;
}

export const ALL_WIDGET_DEFS: WidgetDef[] = [
  { id: 'stat-properties',     label: 'Biens',               icon: 'bi-building',          category: 'Statistiques', size: 'sm' },
  { id: 'stat-rentals',        label: 'Locations actives',   icon: 'bi-key',               category: 'Statistiques', size: 'sm' },
  { id: 'stat-revenue',        label: 'Revenus mensuels',    icon: 'bi-cash-stack',        category: 'Statistiques', size: 'sm' },
  { id: 'stat-pending',        label: 'En attente',          icon: 'bi-clock-history',     category: 'Statistiques', size: 'sm' },
  { id: 'action-items',        label: 'À traiter',           icon: 'bi-bell',              category: 'Gestion',      size: 'lg' },
  { id: 'recent-activity',     label: 'Activité récente',    icon: 'bi-clock-history',     category: 'Gestion',      size: 'lg' },
  { id: 'top-tenants',         label: 'Locataires potentiels', icon: 'bi-people',          category: 'Découverte',   size: 'lg' },
  { id: 'shortcut-properties', label: 'Mes Biens',           icon: 'bi-building',          category: 'Raccourcis',   size: 'sm', route: '/properties', color: 'primary' },
  { id: 'shortcut-rentals',    label: 'Locations',           icon: 'bi-key',               category: 'Raccourcis',   size: 'sm', route: '/rentals',    color: 'success' },
  { id: 'shortcut-contracts',  label: 'Contrats',            icon: 'bi-file-earmark-text', category: 'Raccourcis',   size: 'sm', route: '/contracts',  color: 'info'    },
  { id: 'shortcut-payments',   label: 'Paiements',           icon: 'bi-credit-card',       category: 'Raccourcis',   size: 'sm', route: '/payments',   color: 'warning' },
  { id: 'shortcut-listings',   label: 'Annonces',            icon: 'bi-megaphone',         category: 'Raccourcis',   size: 'sm', route: '/listings',   color: 'neutral' },
  { id: 'shortcut-invoices',   label: 'Factures',            icon: 'bi-receipt',           category: 'Raccourcis',   size: 'sm', route: '/invoices',   color: 'danger'  },
  { id: 'shortcut-expenses',   label: 'Dépenses',            icon: 'bi-wallet2',           category: 'Raccourcis',   size: 'sm', route: '/expenses',   color: 'warning' },
  { id: 'shortcut-messages',   label: 'Messages',            icon: 'bi-chat-dots',         category: 'Raccourcis',   size: 'sm', route: '/messages',   color: 'neutral' },
  { id: 'shortcut-financial',  label: 'Finances',            icon: 'bi-graph-up',          category: 'Raccourcis',   size: 'sm', route: '/financial',  color: 'success' },
];

export const FINANCIAL_WIDGET_DEFS: WidgetDef[] = [
  { id: 'kpi-revenue',        label: 'Revenus totaux',         icon: 'bi-arrow-down-circle',  category: 'Indicateurs', size: 'sm' },
  { id: 'kpi-expenses',       label: 'Dépenses totales',       icon: 'bi-arrow-up-circle',    category: 'Indicateurs', size: 'sm' },
  { id: 'kpi-net',            label: 'Revenu net',             icon: 'bi-graph-up-arrow',     category: 'Indicateurs', size: 'sm' },
  { id: 'kpi-pending',        label: 'En attente',             icon: 'bi-clock-history',      category: 'Indicateurs', size: 'sm' },
  { id: 'kpi-overdue',        label: 'En retard',              icon: 'bi-exclamation-circle', category: 'Indicateurs', size: 'sm' },
  { id: 'monthly-chart',      label: 'Évolution mensuelle',    icon: 'bi-bar-chart',          category: 'Graphiques',  size: 'lg' },
  { id: 'expense-categories', label: 'Dépenses par catégorie', icon: 'bi-pie-chart',          category: 'Graphiques',  size: 'lg' },
];
