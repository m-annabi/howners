import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { OwnerProfileComponent } from './owner-profile/owner-profile.component';

@NgModule({
  declarations: [OwnerProfileComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    RouterModule.forChild([
      { path: ':id', component: OwnerProfileComponent }
    ])
  ]
})
export class OwnersModule {}
