import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

import { RentalListComponent } from './rental-list/rental-list.component';
import { RentalFormComponent } from './rental-form/rental-form.component';
import { RentalDetailComponent } from './rental-detail/rental-detail.component';
import { RentRevisionPanelComponent } from './rent-revision-panel/rent-revision-panel.component';
import { ChargeRegularisationPanelComponent } from './charge-regularisation-panel/charge-regularisation-panel.component';

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
    RentalDetailComponent,
    RentRevisionPanelComponent,
    ChargeRegularisationPanelComponent
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
