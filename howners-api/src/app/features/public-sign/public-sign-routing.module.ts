import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PublicSignComponent } from './public-sign.component';

const routes: Routes = [
  {
    path: '',
    component: PublicSignComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PublicSignRoutingModule { }
