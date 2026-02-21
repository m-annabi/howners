import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListingSearchComponent } from './listing-search/listing-search.component';
import { ListingDetailComponent } from './listing-detail/listing-detail.component';
import { ListingFormComponent } from './listing-form/listing-form.component';
import { MyListingsComponent } from './my-listings/my-listings.component';

const routes: Routes = [
  { path: '', component: ListingSearchComponent },
  { path: 'my', component: MyListingsComponent },
  { path: 'new', component: ListingFormComponent },
  { path: ':id', component: ListingDetailComponent },
  { path: ':id/edit', component: ListingFormComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ListingsRoutingModule { }
