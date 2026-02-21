import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { InventoryRoutingModule } from './inventory-routing.module';
import { EdlListComponent } from './edl-list/edl-list.component';
import { EdlFormComponent } from './edl-form/edl-form.component';
import { EdlDetailComponent } from './edl-detail/edl-detail.component';

@NgModule({
  declarations: [
    EdlListComponent,
    EdlFormComponent,
    EdlDetailComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    InventoryRoutingModule
  ]
})
export class InventoryModule { }
