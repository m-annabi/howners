import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { TenantSearchRoutingModule } from './tenant-search-routing.module';
import { SearchProfileFormComponent } from './search-profile-form/search-profile-form.component';
import { MyInvitationsComponent } from './my-invitations/my-invitations.component';

@NgModule({
  declarations: [
    SearchProfileFormComponent,
    MyInvitationsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    TenantSearchRoutingModule
  ]
})
export class TenantSearchModule { }
