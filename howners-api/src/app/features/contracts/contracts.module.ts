import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';
import { ContractsRoutingModule } from './contracts-routing.module';
import { SharedModule } from '../../shared/shared.module';
import { TemplatesModule } from '../templates/templates.module';
import { ContractListComponent } from './contract-list/contract-list.component';
import { ContractDetailComponent } from './contract-detail/contract-detail.component';
import { ContractFormComponent } from './contract-form/contract-form.component';
import { ContractCustomizeComponent } from './contract-customize/contract-customize.component';
import { ContractSignDialogComponent } from './contract-sign-dialog/contract-sign-dialog.component';
import { AmendmentListComponent } from './amendment-list/amendment-list.component';
import { AmendmentFormComponent } from './amendment-form/amendment-form.component';
import { SignatureDashboardComponent } from './signature-dashboard/signature-dashboard.component';

@NgModule({
  declarations: [
    ContractListComponent,
    ContractDetailComponent,
    ContractFormComponent,
    ContractCustomizeComponent,
    ContractSignDialogComponent,
    AmendmentListComponent,
    AmendmentFormComponent,
    SignatureDashboardComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    QuillModule.forRoot(),
    ContractsRoutingModule,  // Routing AVANT les autres modules
    TemplatesModule  // Import pour utiliser VariableHelperComponent
  ]
})
export class ContractsModule { }
