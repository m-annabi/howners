import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'properties',
    loadChildren: () => import('./features/properties/properties.module').then(m => m.PropertiesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'rentals',
    loadChildren: () => import('./features/rentals/rentals.module').then(m => m.RentalsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'contracts',
    loadChildren: () => import('./features/contracts/contracts.module').then(m => m.ContractsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'templates',
    loadChildren: () => import('./features/templates/templates.module').then(m => m.TemplatesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/profile.module').then(m => m.ProfileModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'ratings',
    loadChildren: () => import('./features/ratings/ratings.module').then(m => m.RatingsModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['OWNER', 'CONCIERGE', 'ADMIN'] }
  },
  {
    path: 'payments',
    loadChildren: () => import('./features/payments/payments.module').then(m => m.PaymentsModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['OWNER', 'TENANT', 'ADMIN'] }
  },
  {
    path: 'invoices',
    loadChildren: () => import('./features/invoices/invoices.module').then(m => m.InvoicesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'receipts',
    loadChildren: () => import('./features/receipts/receipts.module').then(m => m.ReceiptsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'expenses',
    loadChildren: () => import('./features/expenses/expenses.module').then(m => m.ExpensesModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['OWNER', 'ADMIN'] }
  },
  {
    path: 'financial',
    loadChildren: () => import('./features/financial/financial.module').then(m => m.FinancialModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['OWNER', 'ADMIN'] }
  },
  {
    path: 'inventory',
    loadChildren: () => import('./features/inventory/inventory.module').then(m => m.InventoryModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'audit',
    loadChildren: () => import('./features/audit/audit.module').then(m => m.AuditModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'search-profile',
    loadChildren: () => import('./features/tenant-search/tenant-search.module').then(m => m.TenantSearchModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['TENANT', 'ADMIN'] }
  },
  {
    path: 'tenant-discovery',
    loadChildren: () => import('./features/tenant-discovery/tenant-discovery.module').then(m => m.TenantDiscoveryModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['OWNER', 'ADMIN'] }
  },
  {
    path: 'listings',
    loadChildren: () => import('./features/listings/listings.module').then(m => m.ListingsModule)
    // Pas d'AuthGuard pour la recherche publique, les routes owner sont protégées par @PreAuthorize
  },
  {
    path: 'applications',
    loadChildren: () => import('./features/applications/applications.module').then(m => m.ApplicationsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'messages',
    loadChildren: () => import('./features/messages/messages.module').then(m => m.MessagesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'billing',
    loadChildren: () => import('./features/billing/billing.module').then(m => m.BillingModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'sign',
    loadChildren: () => import('./features/public-sign/public-sign.module').then(m => m.PublicSignModule)
    // Pas d'AuthGuard - route publique
  },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
