import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RentalService } from '../rental.service';
import { PropertyService } from '../../properties/property.service';
import { RentalStatus, RENTAL_STATUS_LABELS } from '../../../core/models/rental.model';
import { Property } from '../../../core/models/property.model';

@Component({
  selector: 'app-rental-form',
  templateUrl: './rental-form.component.html',
  styleUrls: ['./rental-form.component.scss']
})
export class RentalFormComponent implements OnInit {
  rentalForm: FormGroup;
  loading = false;
  error: string | null = null;
  rentalId: string | null = null;
  isEditMode = false;
  properties: Property[] = [];

  rentalStatuses = Object.keys(RentalStatus).map(key => ({
    value: RentalStatus[key as keyof typeof RentalStatus],
    label: RENTAL_STATUS_LABELS[RentalStatus[key as keyof typeof RentalStatus]]
  }));

  constructor(
    private fb: FormBuilder,
    private rentalService: RentalService,
    private propertyService: PropertyService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.rentalForm = this.fb.group({
      propertyId: ['', [Validators.required]],
      startDate: [''],
      endDate: [''],
      monthlyRent: [null, [Validators.required, Validators.min(0.01)]],
      currency: ['EUR'],
      depositAmount: [null, [Validators.min(0)]],
      charges: [null, [Validators.min(0)]],
      paymentDay: [1, [Validators.min(1), Validators.max(31)]],
      assuranceExpiration: [''],
      status: [null]
    });
  }

  ngOnInit(): void {
    this.rentalId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.rentalId;
    this.loadProperties();
    if (this.isEditMode && this.rentalId) {
      this.loadRental(this.rentalId);
    }
  }

  loadProperties(): void {
    this.propertyService.getProperties().subscribe({
      next: (page) => { this.properties = page.content; },
      error: () => { this.error = 'Erreur lors du chargement des biens'; }
    });
  }

  loadRental(id: string): void {
    this.loading = true;
    this.rentalService.getRental(id).subscribe({
      next: (rental) => {
        this.rentalForm.patchValue({
          propertyId: rental.propertyId,
          startDate: rental.startDate,
          endDate: rental.endDate,
          monthlyRent: rental.monthlyRent,
          currency: rental.currency,
          depositAmount: rental.depositAmount,
          charges: rental.charges,
          paymentDay: rental.paymentDay,
          assuranceExpiration: rental.assuranceExpiration,
          status: rental.status
        });
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement de la location';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.rentalForm.invalid) return;
    this.loading = true;
    this.error = null;

    const v = this.rentalForm.value;
    const request: any = {
      propertyId: v.propertyId,
      startDate: v.startDate || undefined,
      endDate: v.endDate || undefined,
      monthlyRent: v.monthlyRent,
      currency: v.currency || 'EUR',
      depositAmount: v.depositAmount,
      charges: v.charges,
      paymentDay: v.paymentDay,
      assuranceExpiration: v.assuranceExpiration || undefined
    };

    if (this.isEditMode) {
      if (v.status) request.status = v.status;
      this.rentalService.updateRental(this.rentalId!, request).subscribe({
        next: (rental) => this.router.navigate(['/rentals', rental.id]),
        error: (err) => { this.error = err.error?.message || 'Erreur lors de la mise à jour'; this.loading = false; }
      });
    } else {
      this.rentalService.createRental(request).subscribe({
        next: (rental) => this.router.navigate(['/rentals', rental.id]),
        error: (err) => { this.error = err.error?.message || 'Erreur lors de la création'; this.loading = false; }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/rentals']);
  }
}
