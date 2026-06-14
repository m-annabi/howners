import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DelegationListComponent } from './delegation-list/delegation-list.component';
import { ApercuCompteComponent } from './apercu-compte/apercu-compte.component';

const routes: Routes = [
  { path: '', component: DelegationListComponent },
  { path: ':id/apercu', component: ApercuCompteComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DelegationsRoutingModule { }
