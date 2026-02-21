import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ExpenseListComponent } from './expense-list/expense-list.component';
import { ExpenseFormComponent } from './expense-form/expense-form.component';
import { ExpenseDetailComponent } from './expense-detail/expense-detail.component';

const routes: Routes = [
  { path: '', component: ExpenseListComponent },
  { path: 'new', component: ExpenseFormComponent },
  { path: ':id', component: ExpenseDetailComponent },
  { path: ':id/edit', component: ExpenseFormComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ExpensesRoutingModule { }
