import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PublicSignRoutingModule } from './public-sign-routing.module';
import { PublicSignComponent } from './public-sign.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    PublicSignComponent
  ],
  imports: [
    CommonModule,
    PublicSignRoutingModule,
    SharedModule
  ]
})
export class PublicSignModule { }
