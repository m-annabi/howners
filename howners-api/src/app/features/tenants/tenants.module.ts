import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { TenantProfileComponent } from './tenant-profile/tenant-profile.component';

@NgModule({
  declarations: [TenantListComponent, TenantProfileComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    RouterModule.forChild([
      { path: '', component: TenantListComponent },
      { path: ':id', component: TenantProfileComponent }
    ])
  ]
})
export class TenantsModule {}
