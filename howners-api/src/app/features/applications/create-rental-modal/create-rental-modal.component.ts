import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Application, CreateRentalFromApplicationRequest } from '../../../core/models/application.model';
import { Rental, RentalType, RENTAL_TYPE_LABELS } from '../../../core/models/rental.model';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector: 'app-create-rental-modal',
  templateUrl: './create-rental-modal.component.html'
})
export class CreateRentalModalComponent implements OnInit {
  @Input() application!: Application;
  @Output() onConfirm = new EventEmitter<Rental>();
  @Output() onCancel = new EventEmitter<void>();

  rentalForm!: FormGroup;
  submitting = false;
  error: string | null = null;

  rentalTypes = Object.values(RentalType);
  rentalTypeLabels = RENTAL_TYPE_LABELS;

  constructor(
    private fb: FormBuilder,
    private applicationService: ApplicationService
  ) {}

  ngOnInit(): void {
    this.rentalForm = this.fb.group({
      rentalType: [RentalType.LONG_TERM, Validators.required],
      startDate: [this.application.desiredMoveIn || '', Validators.required],
      endDate: [''],
      monthlyRent: [this.application.listingPricePerMonth || '', [Validators.required, Validators.min(0.01)]],
      currency: [this.application.listingCurrency || 'EUR'],
      depositAmount: [null, [Validators.min(0)]],
      charges: [null, [Validators.min(0)]],
      paymentDay: [1, [Validators.min(1), Validators.max(31)]]
    });
  }

  get dateRangeInvalid(): boolean {
    const start = this.rentalForm?.get('startDate')?.value;
    const end = this.rentalForm?.get('endDate')?.value;
    return !!start && !!end && end < start;
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && !this.submitting) {
      this.cancel();
    }
  }

  confirm(): void {
    if (this.rentalForm.invalid || this.dateRangeInvalid) {
      this.rentalForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.error = null;

    const formValue = this.rentalForm.value;
    const request: CreateRentalFromApplicationRequest = {
      rentalType: formValue.rentalType,
      startDate: formValue.startDate,
      endDate: formValue.endDate || undefined,
      monthlyRent: formValue.monthlyRent,
      currency: formValue.currency || undefined,
      depositAmount: formValue.depositAmount || undefined,
      charges: formValue.charges || undefined,
      paymentDay: formValue.paymentDay || undefined
    };

    this.applicationService.createRentalFromApplication(this.application.id, request).subscribe({
      next: (rental) => {
        this.submitting = false;
        this.onConfirm.emit(rental);
      },
      error: (err) => {
        console.error('Error creating rental from application:', err);
        this.error = err.error?.message || 'Erreur lors de la creation de la location';
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.onCancel.emit();
  }
}
