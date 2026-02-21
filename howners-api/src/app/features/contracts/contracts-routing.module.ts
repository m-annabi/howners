import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContractListComponent } from './contract-list/contract-list.component';
import { ContractDetailComponent } from './contract-detail/contract-detail.component';
import { ContractFormComponent } from './contract-form/contract-form.component';
import { ContractCustomizeComponent } from './contract-customize/contract-customize.component';
import { AmendmentListComponent } from './amendment-list/amendment-list.component';
import { AmendmentFormComponent } from './amendment-form/amendment-form.component';
import { SignatureDashboardComponent } from './signature-dashboard/signature-dashboard.component';

const routes: Routes = [
  {
    path: '',
    component: ContractListComponent
  },
  {
    path: 'new',
    component: ContractFormComponent
  },
  {
    path: 'customize',
    component: ContractCustomizeComponent
  },
  {
    path: 'signatures',
    component: SignatureDashboardComponent
  },
  {
    path: ':id/edit',
    component: ContractCustomizeComponent
  },
  {
    path: ':id',
    component: ContractDetailComponent
  },
  {
    path: ':id/amendments',
    component: AmendmentListComponent
  },
  {
    path: ':id/amendments/new',
    component: AmendmentFormComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ContractsRoutingModule { }
