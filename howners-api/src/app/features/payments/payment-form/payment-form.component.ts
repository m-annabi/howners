import { NavigationService } from '../../../core/services/navigation.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private rentalService: RentalService,
    private notificationService: NotificationService,
    private nav: NavigationService
  ) {}

  ngOnInit(): void {
    this.paymentForm = this.fb.group({
      rentalId: ['', Validators.required],
      paymentType: [PaymentType.RENT, Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      currency: ['EUR'],
      dueDate: ['']
    });

    // Arrivée depuis la fiche d'un bail : le bail est présélectionné.
    const rentalId = this.route.snapshot.queryParamMap.get('rentalId');
    if (rentalId) this.paymentForm.patchValue({ rentalId });

    this.loadRentals();
  }

  loadRentals(): void {
    this.loadingRentals = true;
    this.rentalService.getRentals().pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.rentals = page.content;
        this.loadingRentals = false;
      },
      error: () => {
        this.loadingRentals = false;
      }
    });
  }

  get selectedRental(): any | null {
    const id = this.paymentForm?.get('rentalId')?.value;
    return this.rentals.find(r => r.id === id) ?? null;
  }

  /** Loyer charges comprises attendu pour un paiement RENT, null sinon. */
  get expectedRentAmount(): number | null {
    const rental = this.selectedRental;
    if (!rental || this.paymentForm.get('paymentType')?.value !== PaymentType.RENT) return null;
    if (rental.monthlyRent == null) return null;
    return rental.monthlyRent + (rental.charges ?? 0);
  }

  get amountMismatch(): boolean {
    const expected = this.expectedRentAmount;
    const amount = Number(this.paymentForm.get('amount')?.value);
    return expected !== null && !!amount && amount !== expected;
  }

  applyExpectedAmount(): void {
    if (this.expectedRentAmount !== null) {
      this.paymentForm.patchValue({ amount: this.expectedRentAmount });
    }
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
        this.submitting = false;
        this.notificationService.error(err.error?.message || 'Erreur lors de la création du paiement');
      }
    });
  }

  goBack(): void {
    this.nav.back(['/payments']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
