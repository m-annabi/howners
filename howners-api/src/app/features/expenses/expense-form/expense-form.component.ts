import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ExpenseService } from '../../../core/services/expense.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ExpenseCategory, EXPENSE_CATEGORY_LABELS } from '../../../core/models/expense.model';

@Component({
  selector: 'app-expense-form',
  templateUrl: './expense-form.component.html',
  styles: []
})
export class ExpenseFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  expenseForm!: FormGroup;
  submitting = false;
  isEditMode = false;
  expenseId: string | null = null;
  selectedFile: File | null = null;
  properties: any[] = [];

  categories = Object.values(ExpenseCategory).map(c => ({
    value: c,
    label: EXPENSE_CATEGORY_LABELS[c]
  }));

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private expenseService: ExpenseService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.expenseForm = this.fb.group({
      propertyId: [''],
      rentalId: [''],
      category: [ExpenseCategory.OTHER, Validators.required],
      description: [''],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      expenseDate: ['', Validators.required]
    });

    this.expenseId = this.route.snapshot.paramMap.get('id');
    if (this.expenseId) {
      this.isEditMode = true;
      this.loadExpense(this.expenseId);
    }
  }

  loadExpense(id: string): void {
    this.expenseService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (expense) => {
        this.expenseForm.patchValue({
          propertyId: expense.propertyId || '',
          rentalId: expense.rentalId || '',
          category: expense.category,
          description: expense.description || '',
          amount: expense.amount,
          expenseDate: expense.expenseDate
        });
      },
      error: (err) => {
        console.error('Error loading expense:', err);
        this.notificationService.error('Erreur lors du chargement');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  onSubmit(): void {
    if (this.expenseForm.invalid) return;

    this.submitting = true;
    const formValue = this.expenseForm.value;

    if (this.isEditMode && this.expenseId) {
      this.expenseService.update(this.expenseId, formValue).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.submitting = false;
          this.notificationService.success('Dépense modifiée');
          this.router.navigate(['/expenses', this.expenseId]);
        },
        error: (err) => {
          this.submitting = false;
          this.notificationService.error(err.error?.message || 'Erreur lors de la modification');
        }
      });
    } else {
      this.expenseService.create(formValue, this.selectedFile || undefined).pipe(takeUntil(this.destroy$)).subscribe({
        next: (expense) => {
          this.submitting = false;
          this.notificationService.success('Dépense créée');
          this.router.navigate(['/expenses', expense.id]);
        },
        error: (err) => {
          this.submitting = false;
          this.notificationService.error(err.error?.message || 'Erreur lors de la création');
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/expenses']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
