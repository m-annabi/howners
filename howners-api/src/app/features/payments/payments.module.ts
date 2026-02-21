import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { PaymentsRoutingModule } from './payments-routing.module';
import { PaymentListComponent } from './payment-list/payment-list.component';
import { PaymentDetailComponent } from './payment-detail/payment-detail.component';
import { PaymentFormComponent } from './payment-form/payment-form.component';

@NgModule({
  declarations: [
    PaymentListComponent,
    PaymentDetailComponent,
    PaymentFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    PaymentsRoutingModule
  ]
})
export class PaymentsModule { }
