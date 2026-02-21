import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { AuditRoutingModule } from './audit-routing.module';
import { AuditLogListComponent } from './audit-log-list/audit-log-list.component';

@NgModule({
  declarations: [
    AuditLogListComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    AuditRoutingModule
  ]
})
export class AuditModule { }
