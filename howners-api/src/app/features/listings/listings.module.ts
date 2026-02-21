import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { ListingsRoutingModule } from './listings-routing.module';
import { ListingSearchComponent } from './listing-search/listing-search.component';
import { ListingDetailComponent } from './listing-detail/listing-detail.component';
import { ListingFormComponent } from './listing-form/listing-form.component';
import { MyListingsComponent } from './my-listings/my-listings.component';

@NgModule({
  declarations: [
    ListingSearchComponent,
    ListingDetailComponent,
    ListingFormComponent,
    MyListingsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    ListingsRoutingModule
  ]
})
export class ListingsModule { }
