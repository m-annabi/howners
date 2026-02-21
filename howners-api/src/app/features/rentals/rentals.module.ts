import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

import { RentalListComponent } from './rental-list/rental-list.component';
import { RentalFormComponent } from './rental-form/rental-form.component';
import { RentalDetailComponent } from './rental-detail/rental-detail.component';

const routes: Routes = [
  { path: '', component: RentalListComponent },
  { path: 'new', component: RentalFormComponent },
  { path: ':id', component: RentalDetailComponent },
  { path: ':id/edit', component: RentalFormComponent }
];

@NgModule({
  declarations: [
    RentalListComponent,
    RentalFormComponent,
    RentalDetailComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class RentalsModule { }
