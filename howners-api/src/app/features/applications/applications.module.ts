import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { ApplicationsRoutingModule } from './applications-routing.module';
import { MyApplicationsComponent } from './my-applications/my-applications.component';
import { ApplicationListComponent } from './application-list/application-list.component';
import { ApplicationFormComponent } from './application-form/application-form.component';
import { CreateRentalModalComponent } from './create-rental-modal/create-rental-modal.component';

@NgModule({
  declarations: [
    MyApplicationsComponent,
    ApplicationListComponent,
    ApplicationFormComponent,
    CreateRentalModalComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    ApplicationsRoutingModule
  ]
})
export class ApplicationsModule { }
