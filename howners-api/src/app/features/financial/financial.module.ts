import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { FinancialRoutingModule } from './financial-routing.module';
import { FinancialDashboardComponent } from './financial-dashboard/financial-dashboard.component';

@NgModule({
  declarations: [
    FinancialDashboardComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    FinancialRoutingModule
  ]
})
export class FinancialModule { }
