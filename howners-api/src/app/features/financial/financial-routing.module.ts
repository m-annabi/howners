import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FinancialDashboardComponent } from './financial-dashboard/financial-dashboard.component';

const routes: Routes = [
  { path: '', component: FinancialDashboardComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FinancialRoutingModule { }
