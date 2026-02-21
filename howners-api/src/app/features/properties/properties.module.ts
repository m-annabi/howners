import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

import { PropertyListComponent } from './property-list/property-list.component';
import { PropertyFormComponent } from './property-form/property-form.component';
import { PropertyDetailComponent } from './property-detail/property-detail.component';
import { PropertyProfitabilityComponent } from './property-profitability/property-profitability.component';

const routes: Routes = [
  { path: '', component: PropertyListComponent },
  { path: 'new', component: PropertyFormComponent },
  { path: ':id', component: PropertyDetailComponent },
  { path: ':id/edit', component: PropertyFormComponent }
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
