import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FinancialDashboardComponent } from './financial-dashboard/financial-dashboard.component';
import { FiscalExportComponent } from './fiscal-export/fiscal-export.component';
import { PatrimoineComponent } from './patrimoine/patrimoine.component';
import { PlanGuard } from '../../core/guards/plan.guard';

const routes: Routes = [
  { path: '', component: FinancialDashboardComponent },
  { path: 'fiscal-2044', component: FiscalExportComponent, canActivate: [PlanGuard], data: { proFeature: 'fiscal-2044' } },
  { path: 'patrimoine', component: PatrimoineComponent, canActivate: [PlanGuard], data: { proFeature: 'patrimoine' } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FinancialRoutingModule { }
