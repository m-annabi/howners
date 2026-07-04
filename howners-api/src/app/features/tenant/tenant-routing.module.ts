import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TenantDashboardComponent } from './dashboard/tenant-dashboard.component';
import { TenantDossierComponent } from './dossier/tenant-dossier.component';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: TenantDashboardComponent },
  { path: 'dossier', component: TenantDossierComponent },
  // La page « Mes avis » est désormais commune aux deux rôles
  { path: 'avis', redirectTo: '/avis' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TenantRoutingModule {}
