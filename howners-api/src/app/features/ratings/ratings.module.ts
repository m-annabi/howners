import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

import { TenantRatingListComponent } from './tenant-rating-list/tenant-rating-list.component';
import { TenantRatingFormComponent } from './tenant-rating-form/tenant-rating-form.component';
import { TenantScoreComponent } from './tenant-score/tenant-score.component';
import { TenantComparisonComponent } from './tenant-comparison/tenant-comparison.component';

const routes: Routes = [
  { path: '', component: TenantRatingListComponent },
  { path: 'new', component: TenantRatingFormComponent },
  { path: 'score/:tenantId', component: TenantScoreComponent },
  { path: 'compare', component: TenantComparisonComponent },
  { path: ':id/edit', component: TenantRatingFormComponent }
];

@NgModule({
  declarations: [
    TenantRatingListComponent,
    TenantRatingFormComponent,
    TenantScoreComponent,
    TenantComparisonComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class RatingsModule { }
