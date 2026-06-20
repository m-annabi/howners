import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TenantDashboardComponent } from './dashboard/tenant-dashboard.component';
import { TenantDossierComponent } from './dossier/tenant-dossier.component';
import { TenantAvisComponent } from './avis/tenant-avis.component';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: TenantDashboardComponent },
  { path: 'dossier', component: TenantDossierComponent },
  { path: 'avis', component: TenantAvisComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TenantRoutingModule {}
