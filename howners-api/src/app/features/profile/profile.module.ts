import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ProfileRoutingModule } from './profile-routing.module';
import { ProfileComponent } from './profile/profile.component';
import { RgpdSettingsComponent } from './rgpd-settings/rgpd-settings.component';

@NgModule({
  declarations: [
    ProfileComponent,
    RgpdSettingsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ProfileRoutingModule
  ]
})
export class ProfileModule { }
