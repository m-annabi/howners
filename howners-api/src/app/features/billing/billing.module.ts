import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { BillingRoutingModule } from './billing-routing.module';
import { PricingComponent } from './pricing/pricing.component';
import { CurrentPlanComponent } from './current-plan/current-plan.component';
import { CheckoutSuccessComponent } from './checkout-success/checkout-success.component';
import { UpgradePromptComponent } from './upgrade-prompt/upgrade-prompt.component';
import { ProFeatureComponent } from './pro-feature/pro-feature.component';

@NgModule({
  declarations: [
    PricingComponent,
    CurrentPlanComponent,
    CheckoutSuccessComponent,
    UpgradePromptComponent,
    ProFeatureComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    BillingRoutingModule
  ],
  exports: [
    UpgradePromptComponent
  ]
})
export class BillingModule { }
