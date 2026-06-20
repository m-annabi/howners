import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { TenantRoutingModule } from './tenant-routing.module';
import { TenantDashboardComponent } from './dashboard/tenant-dashboard.component';
import { TenantDossierComponent } from './dossier/tenant-dossier.component';
import { TenantAvisComponent } from './avis/tenant-avis.component';

@NgModule({
  declarations: [
    TenantDashboardComponent,
    TenantDossierComponent,
    TenantAvisComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    TenantRoutingModule
  ]
})
export class TenantModule {}
