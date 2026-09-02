import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PublicSignComponent } from './public-sign.component';
import { PublicSignCompleteComponent } from './public-sign-complete/public-sign-complete.component';

const routes: Routes = [
  {
    path: '',
    component: PublicSignComponent
  },
  {
    // URL de retour de la cérémonie de signature externe (DocuSign) : publique, sans AuthGuard.
    path: 'complete',
    component: PublicSignCompleteComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PublicSignRoutingModule { }
