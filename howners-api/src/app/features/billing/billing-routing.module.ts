import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PricingComponent } from './pricing/pricing.component';
import { CurrentPlanComponent } from './current-plan/current-plan.component';
import { CheckoutSuccessComponent } from './checkout-success/checkout-success.component';

const routes: Routes = [
  { path: '', component: CurrentPlanComponent },
  { path: 'pricing', component: PricingComponent },
  { path: 'success', component: CheckoutSuccessComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BillingRoutingModule { }
