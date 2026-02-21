import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EdlListComponent } from './edl-list/edl-list.component';
import { EdlFormComponent } from './edl-form/edl-form.component';
import { EdlDetailComponent } from './edl-detail/edl-detail.component';

const routes: Routes = [
  { path: '', component: EdlListComponent },
  { path: 'new/:rentalId', component: EdlFormComponent },
  { path: ':rentalId/:id', component: EdlDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class InventoryRoutingModule { }
