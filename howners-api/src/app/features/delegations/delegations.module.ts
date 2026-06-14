import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { DelegationsRoutingModule } from './delegations-routing.module';
import { DelegationListComponent } from './delegation-list/delegation-list.component';
import { ApercuCompteComponent } from './apercu-compte/apercu-compte.component';

@NgModule({
  declarations: [
    DelegationListComponent,
    ApercuCompteComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    DelegationsRoutingModule
  ]
})
export class DelegationsModule { }
