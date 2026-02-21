import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RentalService } from '../rental.service';
import { PropertyService } from '../../properties/property.service';
import { RentalType, RentalStatus, RENTAL_TYPE_LABELS, RENTAL_STATUS_LABELS } from '../../../core/models/rental.model';
import { Property } from '../../../core/models/property.model';
import { User } from '../../../core/models/user.model';

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
  tenants: User[] = [];
  createNewTenant = true;

  rentalTypes = Object.keys(RentalType).map(key => ({
    value: RentalType[key as keyof typeof RentalType],
    label: RENTAL_TYPE_LABELS[RentalType[key as keyof typeof RentalType]]
  }));

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
      rentalType: [RentalType.LONG_TERM, [Validators.required]],
      status: [RentalStatus.PENDING],
      startDate: ['', [Validators.required]],
      endDate: [''],
      monthlyRent: [null, [Validators.required, Validators.min(0)]],
      currency: ['EUR'],
      depositAmount: [null, [Validators.min(0)]],
      charges: [null, [Validators.min(0)]],
      paymentDay: [1, [Validators.min(1), Validators.max(31)]],
      // Existing tenant
      tenantId: [''],
      // New tenant info
      tenantEmail: ['', [Validators.required, Validators.email]],
      tenantFirstName: ['', [Validators.required]],
      tenantLastName: ['', [Validators.required]],
      tenantPhone: ['']
    });
  }

  ngOnInit(): void {
    this.rentalId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.rentalId;

    this.loadProperties();
    this.loadTenants();

    if (this.isEditMode && this.rentalId) {
      this.loadRental(this.rentalId);
      // En mode édition, on ne peut pas changer le locataire
      this.createNewTenant = false;
    }
  }

  loadProperties(): void {
    this.propertyService.getProperties().subscribe({
      next: (properties) => {
        this.properties = properties;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des biens';
      }
    });
  }

  loadTenants(): void {
    this.rentalService.getMyTenants().subscribe({
      next: (tenants) => {
        this.tenants = tenants;
      },
      error: (err) => {
        // Non-blocking: tenants list is optional
      }
    });
  }

  loadRental(id: string): void {
    this.loading = true;
    this.rentalService.getRental(id).subscribe({
      next: (rental) => {
        this.rentalForm.patchValue({
          propertyId: rental.propertyId,
          rentalType: rental.rentalType,
          status: rental.status,
          startDate: rental.startDate,
          endDate: rental.endDate,
          monthlyRent: rental.monthlyRent,
          currency: rental.currency,
          depositAmount: rental.depositAmount,
          charges: rental.charges,
          paymentDay: rental.paymentDay
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
    if (this.rentalForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const formValue = this.rentalForm.value;

    const request: any = {
      propertyId: formValue.propertyId,
      rentalType: formValue.rentalType,
      startDate: formValue.startDate,
      endDate: formValue.endDate || undefined,
      monthlyRent: formValue.monthlyRent,
      currency: formValue.currency || 'EUR',
      depositAmount: formValue.depositAmount,
      charges: formValue.charges,
      paymentDay: formValue.paymentDay
    };

    if (this.isEditMode) {
      // Mode édition
      request.status = formValue.status;
      this.rentalService.updateRental(this.rentalId!, request).subscribe({
        next: () => {
          this.router.navigate(['/rentals']);
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour';
          this.loading = false;
        }
      });
    } else {
      // Mode création
      if (this.createNewTenant) {
        // Créer un nouveau locataire
        request.tenantEmail = formValue.tenantEmail;
        request.tenantFirstName = formValue.tenantFirstName;
        request.tenantLastName = formValue.tenantLastName;
        request.tenantPhone = formValue.tenantPhone;
      } else if (formValue.tenantId) {
        // Locataire existant
        request.tenantId = formValue.tenantId;
      }

      this.rentalService.createRental(request).subscribe({
        next: () => {
          this.router.navigate(['/rentals']);
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la création';
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/rentals']);
  }

  toggleTenantMode(): void {
    if (this.createNewTenant && !this.isEditMode) {
      // Mode nouveau locataire : champs requis
      this.rentalForm.get('tenantEmail')?.setValidators([Validators.required, Validators.email]);
      this.rentalForm.get('tenantFirstName')?.setValidators([Validators.required]);
      this.rentalForm.get('tenantLastName')?.setValidators([Validators.required]);
      this.rentalForm.get('tenantId')?.clearValidators();
      this.rentalForm.get('tenantId')?.setValue('');
    } else {
      // Mode locataire existant : select requis
      this.rentalForm.get('tenantEmail')?.clearValidators();
      this.rentalForm.get('tenantFirstName')?.clearValidators();
      this.rentalForm.get('tenantLastName')?.clearValidators();
      this.rentalForm.get('tenantEmail')?.setValue('');
      this.rentalForm.get('tenantFirstName')?.setValue('');
      this.rentalForm.get('tenantLastName')?.setValue('');
      this.rentalForm.get('tenantPhone')?.setValue('');
      this.rentalForm.get('tenantId')?.setValidators([Validators.required]);
    }

    this.rentalForm.get('tenantEmail')?.updateValueAndValidity();
    this.rentalForm.get('tenantFirstName')?.updateValueAndValidity();
    this.rentalForm.get('tenantLastName')?.updateValueAndValidity();
    this.rentalForm.get('tenantId')?.updateValueAndValidity();
  }
}
