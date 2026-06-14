import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { FinancialRoutingModule } from './financial-routing.module';
import { FinancialDashboardComponent } from './financial-dashboard/financial-dashboard.component';
import { FiscalExportComponent } from './fiscal-export/fiscal-export.component';
import { PatrimoineComponent } from './patrimoine/patrimoine.component';

@NgModule({
  declarations: [
    FinancialDashboardComponent,
    FiscalExportComponent,
    PatrimoineComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    FinancialRoutingModule
  ]
})
export class FinancialModule { }
