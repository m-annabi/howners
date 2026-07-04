import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MyAvisComponent } from './my-avis.component';

@NgModule({
  declarations: [MyAvisComponent],
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      { path: '', component: MyAvisComponent }
    ])
  ]
})
export class AvisModule {}
