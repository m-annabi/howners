import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PaymentService } from '../../../core/services/payment.service';
import { NotificationService } from '../../../core/services/notification.service';
import { RentalService } from '../../rentals/rental.service';
import { PaymentType, PAYMENT_TYPE_LABELS } from '../../../core/models/payment.model';

@Component({
  selector: 'app-payment-form',
  templateUrl: './payment-form.component.html',
  styles: []
})
export class PaymentFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  paymentForm!: FormGroup;
  submitting = false;
  rentals: any[] = [];
  loadingRentals = false;

  paymentTypes = Object.values(PaymentType).map(t => ({
    value: t,
    label: PAYMENT_TYPE_LABELS[t]
  }));

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private paymentService: PaymentService,
    private rentalService: RentalService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.paymentForm = this.fb.group({
      rentalId: ['', Validators.required],
      paymentType: [PaymentType.RENT, Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      currency: ['EUR'],
      dueDate: [''],
      paymentMethod: ['BANK_TRANSFER']
    });

    this.loadRentals();
  }

  loadRentals(): void {
    this.loadingRentals = true;
    this.rentalService.getRentals().pipe(takeUntil(this.destroy$)).subscribe({
      next: (rentals) => {
        this.rentals = rentals;
        this.loadingRentals = false;
      },
      error: (err) => {
        console.error('Error loading rentals:', err);
        this.loadingRentals = false;
      }
    });
  }

  onSubmit(): void {
    if (this.paymentForm.invalid) return;

    this.submitting = true;
    this.paymentService.create(this.paymentForm.value).pipe(takeUntil(this.destroy$)).subscribe({
      next: (payment) => {
        this.submitting = false;
        this.notificationService.success('Paiement créé avec succès');
        this.router.navigate(['/payments', payment.id]);
      },
      error: (err) => {
        console.error('Error creating payment:', err);
        this.submitting = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la création du paiement');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/payments']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
