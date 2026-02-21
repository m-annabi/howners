import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PublicSignRoutingModule } from './public-sign-routing.module';
import { PublicSignComponent } from './public-sign.component';

@NgModule({
  declarations: [
    PublicSignComponent
  ],
  imports: [
    CommonModule,
    PublicSignRoutingModule
  ]
})
export class PublicSignModule { }
