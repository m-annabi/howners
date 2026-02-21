import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PaymentListComponent } from './payment-list/payment-list.component';
import { PaymentDetailComponent } from './payment-detail/payment-detail.component';
import { PaymentFormComponent } from './payment-form/payment-form.component';

const routes: Routes = [
  { path: '', component: PaymentListComponent },
  { path: 'new', component: PaymentFormComponent },
  { path: ':id', component: PaymentDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PaymentsRoutingModule { }
