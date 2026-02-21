import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SearchProfileFormComponent } from './search-profile-form/search-profile-form.component';
import { MyInvitationsComponent } from './my-invitations/my-invitations.component';

const routes: Routes = [
  { path: '', component: SearchProfileFormComponent },
  { path: 'invitations', component: MyInvitationsComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TenantSearchRoutingModule { }
