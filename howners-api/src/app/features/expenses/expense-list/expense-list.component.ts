import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ExpenseService } from '../../../core/services/expense.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Expense,
  ExpenseCategory,
  EXPENSE_CATEGORY_LABELS
} from '../../../core/models/expense.model';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

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
  selectedCategory: string = 'ALL';

  categoryLabels = EXPENSE_CATEGORY_LABELS;

  get filters(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('ALL', this.expenses.length);
    for (const e of this.expenses) {
      counts.set(e.category, (counts.get(e.category) || 0) + 1);
    }
    const list: QuickFilter[] = [
      { key: 'ALL', label: 'Toutes', count: counts.get('ALL') || 0 }
    ];
    for (const c of Object.values(ExpenseCategory)) {
      const cnt = counts.get(c) || 0;
      if (cnt > 0) {
        list.push({ key: c, label: this.categoryLabels[c], count: cnt });
      }
    }
    return list;
  }

  onFilterChange(key: string): void {
    this.selectedCategory = key;
    this.applyFilters();
  }

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
