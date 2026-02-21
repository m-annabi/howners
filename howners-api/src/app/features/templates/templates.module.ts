import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';
import { TemplatesRoutingModule } from './templates-routing.module';
import { SharedModule } from '../../shared/shared.module';
import { TemplateListComponent } from './template-list/template-list.component';
import { TemplateFormComponent } from './template-form/template-form.component';
import { VariableHelperComponent } from './variable-helper/variable-helper.component';

@NgModule({
  declarations: [
    TemplateListComponent,
    TemplateFormComponent,
    VariableHelperComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    QuillModule.forRoot(),
    TemplatesRoutingModule
  ],
  exports: [
    VariableHelperComponent  // Export pour réutilisation dans d'autres modules
  ]
})
export class TemplatesModule { }
