import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { TenantDiscoveryRoutingModule } from './tenant-discovery-routing.module';
import { TenantSearchComponent } from './tenant-search/tenant-search.component';
import { TenantProfileDetailComponent } from './tenant-profile-detail/tenant-profile-detail.component';
import { SentInvitationsComponent } from './sent-invitations/sent-invitations.component';

@NgModule({
  declarations: [
    TenantSearchComponent,
    TenantProfileDetailComponent,
    SentInvitationsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    TenantDiscoveryRoutingModule
  ]
})
export class TenantDiscoveryModule { }
