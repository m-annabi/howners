import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { ReceiptsRoutingModule } from './receipts-routing.module';
import { ReceiptListComponent } from './receipt-list/receipt-list.component';
import { ReceiptDetailComponent } from './receipt-detail/receipt-detail.component';

@NgModule({
  declarations: [
    ReceiptListComponent,
    ReceiptDetailComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    ReceiptsRoutingModule
  ]
})
export class ReceiptsModule { }
