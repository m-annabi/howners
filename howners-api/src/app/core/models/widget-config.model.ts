export interface WidgetConfig {
  id: string;
  visible: boolean;
  order: number;
  items?: string[];
}

export type WidgetPage = 'dashboard' | 'financial';

export interface WidgetDefinition {
  id: string;
  label: string;
  icon: string;
}

export interface ShortcutDef {
  id: string;
  label: string;
  desc: string;
  route: string;
  icon: string;
  color: string;
}

export interface OverviewStatDef {
  id: string;
  label: string;
  icon: string;
  color: 'primary' | 'success' | 'warning' | 'danger';
  routerLink: string;
}

export const ALL_OVERVIEW_STATS: OverviewStatDef[] = [
  { id: 'properties', label: 'Biens',            icon: 'bi-building',    color: 'primary', routerLink: '/properties' },
  { id: 'rentals',    label: 'Locations actives', icon: 'bi-key',         color: 'success', routerLink: '/rentals' },
  { id: 'revenue',    label: 'Revenus mensuels',  icon: 'bi-cash-stack',  color: 'warning', routerLink: '/financial' },
  { id: 'pending',    label: 'En attente',        icon: 'bi-clock-history', color: 'danger', routerLink: '/rentals' },
];

export const ALL_SHORTCUTS: ShortcutDef[] = [
  { id: 'properties',        label: 'Mes Biens',  desc: 'Gérer mes propriétés',        route: '/properties',        icon: 'bi-building',         color: 'primary' },
  { id: 'rentals',           label: 'Locations',  desc: 'Gérer mes locations',         route: '/rentals',           icon: 'bi-key',              color: 'success' },
  { id: 'contracts',         label: 'Contrats',   desc: 'Créer et gérer',              route: '/contracts',         icon: 'bi-file-earmark-text', color: 'info' },
  { id: 'payments',          label: 'Paiements',  desc: 'Suivre les paiements',        route: '/payments',          icon: 'bi-credit-card',      color: 'warning' },
  { id: 'listings',          label: 'Annonces',   desc: 'Publier et rechercher',       route: '/listings',          icon: 'bi-megaphone',        color: 'neutral' },
  { id: 'invoices',          label: 'Factures',   desc: 'Gérer la facturation',        route: '/invoices',          icon: 'bi-receipt',          color: 'danger' },
  { id: 'expenses',          label: 'Dépenses',   desc: 'Suivre les dépenses',         route: '/expenses',          icon: 'bi-wallet2',          color: 'warning' },
  { id: 'messages',          label: 'Messages',   desc: 'Mes conversations',           route: '/messages',          icon: 'bi-chat-dots',        color: 'neutral' },
  { id: 'tenant-discovery',  label: 'Locataires', desc: 'Découvrir des profils',       route: '/tenant-discovery',  icon: 'bi-people',           color: 'primary' },
  { id: 'financial',         label: 'Finances',   desc: 'Tableau de bord financier',   route: '/financial',         icon: 'bi-graph-up',         color: 'success' },
];

export const DEFAULT_SHORTCUT_IDS = ['properties', 'rentals', 'contracts', 'payments', 'listings', 'invoices'];
export const DEFAULT_OVERVIEW_STAT_IDS = ['properties', 'rentals', 'revenue', 'pending'];

export const DASHBOARD_WIDGET_DEFS: WidgetDefinition[] = [
  { id: 'overview',        label: 'Vue d\'ensemble',       icon: 'bi-grid-1x2' },
  { id: 'action-items',    label: 'À traiter',             icon: 'bi-bell' },
  { id: 'top-tenants',     label: 'Locataires potentiels', icon: 'bi-people' },
  { id: 'recent-activity', label: 'Activité récente',      icon: 'bi-clock-history' },
  { id: 'quick-actions',   label: 'Raccourcis',            icon: 'bi-lightning' },
];

export const FINANCIAL_WIDGET_DEFS: WidgetDefinition[] = [
  { id: 'financial-kpis',     label: 'Indicateurs clés',      icon: 'bi-graph-up' },
  { id: 'monthly-chart',      label: 'Évolution mensuelle',   icon: 'bi-bar-chart' },
  { id: 'expense-categories', label: 'Dépenses par catégorie', icon: 'bi-pie-chart' },
];
