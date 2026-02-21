import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TenantSearchComponent } from './tenant-search/tenant-search.component';
import { TenantProfileDetailComponent } from './tenant-profile-detail/tenant-profile-detail.component';

const routes: Routes = [
  { path: '', component: TenantSearchComponent },
  { path: ':profileId', component: TenantProfileDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TenantDiscoveryRoutingModule { }
