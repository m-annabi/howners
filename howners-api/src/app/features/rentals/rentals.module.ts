import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RoleGuard } from '../../core/guards/role.guard';

import { RentalListComponent } from './rental-list/rental-list.component';
import { RentalFormComponent } from './rental-form/rental-form.component';
import { RentalDetailComponent } from './rental-detail/rental-detail.component';
import { RentRevisionPanelComponent } from './rent-revision-panel/rent-revision-panel.component';
import { ChargeRegularisationPanelComponent } from './charge-regularisation-panel/charge-regularisation-panel.component';

const routes: Routes = [
  // Liste et détail sont partagés (le locataire y voit « Ma location ») ; seules la
  // création et l'édition d'un bail sont réservées aux propriétaires.
  { path: '', component: RentalListComponent },
  { path: 'new', component: RentalFormComponent, canActivate: [RoleGuard], data: { roles: ['OWNER', 'ADMIN'] } },
  { path: ':id', component: RentalDetailComponent },
  { path: ':id/edit', component: RentalFormComponent, canActivate: [RoleGuard], data: { roles: ['OWNER', 'ADMIN'] } }
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
