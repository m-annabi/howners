import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ReferralComponent } from './referral.component';

const routes: Routes = [{ path: '', component: ReferralComponent }];

@NgModule({
  declarations: [ReferralComponent],
  imports: [CommonModule, SharedModule, RouterModule.forChild(routes)]
})
export class ReferralModule {}
