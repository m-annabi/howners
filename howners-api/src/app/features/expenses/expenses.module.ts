import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { ExpensesRoutingModule } from './expenses-routing.module';
import { ExpenseListComponent } from './expense-list/expense-list.component';
import { ExpenseFormComponent } from './expense-form/expense-form.component';
import { ExpenseDetailComponent } from './expense-detail/expense-detail.component';

@NgModule({
  declarations: [
    ExpenseListComponent,
    ExpenseFormComponent,
    ExpenseDetailComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule,
    ExpensesRoutingModule
  ]
})
export class ExpensesModule { }
