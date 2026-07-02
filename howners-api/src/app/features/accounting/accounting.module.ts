import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AccountingComponent } from './accounting.component';

const routes: Routes = [
  { path: '', component: AccountingComponent }
];

@NgModule({
  declarations: [AccountingComponent],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class AccountingModule {}
