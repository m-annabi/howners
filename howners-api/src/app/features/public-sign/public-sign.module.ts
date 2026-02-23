import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PublicSignRoutingModule } from './public-sign-routing.module';
import { PublicSignComponent } from './public-sign.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    PublicSignComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    PublicSignRoutingModule
  ]
})
export class PublicSignModule { }
