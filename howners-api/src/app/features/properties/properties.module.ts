import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RoleGuard } from '../../core/guards/role.guard';

import { PropertyListComponent } from './property-list/property-list.component';
import { PropertyFormComponent } from './property-form/property-form.component';
import { PropertyDetailComponent } from './property-detail/property-detail.component';
import { PropertyProfitabilityComponent } from './property-profitability/property-profitability.component';

const routes: Routes = [
  // La gestion des biens est réservée aux propriétaires : sans ce guard, un locataire
  // tapant l'URL voyait les formulaires (le refus n'arrivait qu'à la soumission, côté API).
  {
    path: '',
    canActivate: [RoleGuard],
    data: { roles: ['OWNER', 'ADMIN'] },
    children: [
      { path: '', component: PropertyListComponent },
      { path: 'new', component: PropertyFormComponent },
      { path: ':id', component: PropertyDetailComponent },
      { path: ':id/edit', component: PropertyFormComponent }
    ]
  }
];

@NgModule({
  declarations: [
    PropertyListComponent,
    PropertyFormComponent,
    PropertyDetailComponent,
    PropertyProfitabilityComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class PropertiesModule { }
