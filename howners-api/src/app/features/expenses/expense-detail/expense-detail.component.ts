import { NavigationService } from '../../../core/services/navigation.service';
import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ExpenseService } from '../../../core/services/expense.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Expense, EXPENSE_CATEGORY_LABELS } from '../../../core/models/expense.model';

@Component({
  selector: 'app-expense-detail',
  templateUrl: './expense-detail.component.html',
  styles: []
})
export class ExpenseDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  expense: Expense | null = null;
  loading = false;
  error: string | null = null;

  categoryLabels = EXPENSE_CATEGORY_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private expenseService: ExpenseService,
    private notificationService: NotificationService,
    private confirmDialog: ConfirmDialogService,
    private nav: NavigationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadExpense(id);
    }
  }

  loadExpense(id: string): void {
    this.loading = true;
    this.expenseService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (expense) => {
        this.expense = expense;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement de la dépense';
        this.loading = false;
      }
    });
  }

  editExpense(): void {
    if (this.expense) {
      this.router.navigate(['/expenses', this.expense.id, 'edit']);
    }
  }

  deleteExpense(): void {
    if (!this.expense) return;
    this.confirmDialog.confirm('Confirmer la suppression', 'Supprimer cette dépense ?', 'danger').subscribe(ok => {
      if (!ok) return;
      this.expenseService.delete(this.expense!.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.notificationService.success('Dépense supprimée');
          this.goBack();
        },
        error: () => {
          this.notificationService.error('Erreur lors de la suppression');
        }
      });
    });
  }

  goBack(): void {
    this.nav.back(['/expenses']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
