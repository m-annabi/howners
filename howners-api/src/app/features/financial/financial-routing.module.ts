import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FinancialDashboardComponent } from './financial-dashboard/financial-dashboard.component';
import { FiscalExportComponent } from './fiscal-export/fiscal-export.component';
import { PatrimoineComponent } from './patrimoine/patrimoine.component';

const routes: Routes = [
  { path: '', component: FinancialDashboardComponent },
  { path: 'fiscal-2044', component: FiscalExportComponent },
  { path: 'patrimoine', component: PatrimoineComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FinancialRoutingModule { }
