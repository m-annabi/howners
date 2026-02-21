import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ExpenseService } from '../../../core/services/expense.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Expense,
  ExpenseCategory,
  EXPENSE_CATEGORY_LABELS
} from '../../../core/models/expense.model';

@Component({
  selector: 'app-expense-list',
  templateUrl: './expense-list.component.html',
  styleUrls: ['./expense-list.component.scss']
})
export class ExpenseListComponent implements OnInit {
  expenses: Expense[] = [];
  filteredExpenses: Expense[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedCategory: ExpenseCategory | 'ALL' = 'ALL';

  categoryLabels = EXPENSE_CATEGORY_LABELS;

  categories = [
    { value: 'ALL', label: 'Toutes les catégories' },
    ...Object.values(ExpenseCategory).map(c => ({ value: c, label: EXPENSE_CATEGORY_LABELS[c] }))
  ];

  constructor(
    private expenseService: ExpenseService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadExpenses();
  }

  loadExpenses(): void {
    this.loading = true;
    this.error = null;

    this.expenseService.getAll().subscribe({
      next: (expenses) => {
        this.expenses = expenses;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading expenses:', err);
        this.error = 'Erreur lors du chargement des dépenses';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.expenses;

    if (this.selectedCategory !== 'ALL') {
      filtered = filtered.filter(e => e.category === this.selectedCategory);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(e =>
        (e.propertyName?.toLowerCase().includes(term) || false) ||
        (e.description?.toLowerCase().includes(term) || false)
      );
    }

    this.filteredExpenses = filtered;
  }

  onCategoryChange(): void { this.applyFilters(); }
  onSearchChange(): void { this.applyFilters(); }

  getTotalExpenses(): number {
    return this.filteredExpenses.reduce((sum, e) => sum + e.amount, 0);
  }

  viewExpense(expense: Expense): void {
    this.router.navigate(['/expenses', expense.id]);
  }

  createExpense(): void {
    this.router.navigate(['/expenses/new']);
  }

  deleteExpense(expense: Expense, event: Event): void {
    event.stopPropagation();
    if (confirm('Supprimer cette dépense ?')) {
      this.expenseService.delete(expense.id).subscribe({
        next: () => {
          this.notificationService.success('Dépense supprimée');
          this.loadExpenses();
        },
        error: (err) => {
          console.error('Error deleting expense:', err);
          this.notificationService.error('Erreur lors de la suppression');
        }
      });
    }
  }
}
