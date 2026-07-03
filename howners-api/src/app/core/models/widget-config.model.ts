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
  { id: 'stat-properties',    label: 'Biens',              icon: 'bi-building',         category: 'Statistiques', size: 'sm' },
  { id: 'stat-rentals',       label: 'Locations actives',  icon: 'bi-key',              category: 'Statistiques', size: 'sm' },
  { id: 'stat-revenue',       label: 'Revenus mensuels',   icon: 'bi-cash-stack',       category: 'Statistiques', size: 'sm' },
  { id: 'stat-pending',       label: 'En attente',         icon: 'bi-clock-history',    category: 'Statistiques', size: 'sm' },
  { id: 'action-items',       label: 'À traiter',          icon: 'bi-bell',             category: 'Gestion',      size: 'lg' },
  { id: 'recent-activity',    label: 'Activité récente',   icon: 'bi-clock-history',    category: 'Gestion',      size: 'lg' },
  { id: 'quick-links',        label: 'Accès rapides',      icon: 'bi-lightning-charge', category: 'Raccourcis',   size: 'lg' },
  { id: 'financial-analysis', label: 'Analyse financière', icon: 'bi-graph-up',         category: 'Finances',     size: 'lg' },
];

/** Liens du widget « Accès rapides » (ex-tuiles de raccourcis individuelles). */
export const QUICK_LINKS: { label: string; icon: string; route: string }[] = [
  { label: 'Mes Biens', icon: 'bi-building',          route: '/properties' },
  { label: 'Locations', icon: 'bi-key',               route: '/rentals' },
  { label: 'Contrats',  icon: 'bi-file-earmark-text', route: '/contracts' },
  { label: 'Paiements', icon: 'bi-credit-card',       route: '/payments' },
  { label: 'Annonces',  icon: 'bi-megaphone',         route: '/listings' },
  { label: 'Factures',  icon: 'bi-receipt',           route: '/invoices' },
  { label: 'Dépenses',  icon: 'bi-wallet2',           route: '/expenses' },
  { label: 'Messages',  icon: 'bi-chat-dots',         route: '/messages' },
  { label: 'Finances',  icon: 'bi-graph-up',          route: '/financial' },
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
