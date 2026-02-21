import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TenantRatingService } from '../../../core/services/tenant-rating.service';
import { RentalService } from '../../rentals/rental.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Rental } from '../../../core/models/rental.model';
import { RATING_CRITERIA_LABELS, RATING_CRITERIA_ICONS } from '../../../core/models/tenant-rating.model';

@Component({
  selector: 'app-tenant-rating-form',
  templateUrl: './tenant-rating-form.component.html',
  styleUrls: ['./tenant-rating-form.component.css']
})
export class TenantRatingFormComponent implements OnInit {
  ratingForm: FormGroup;
  loading = false;
  error: string | null = null;
  ratingId: string | null = null;
  isEditMode = false;

  rentals: Rental[] = [];
  loadingRentals = true;

  criteriaLabels = RATING_CRITERIA_LABELS;
  criteriaIcons = RATING_CRITERIA_ICONS;

  // Star rating state
  paymentStars = 0;
  propertyRespectStars = 0;
  communicationStars = 0;

  paymentHover = 0;
  propertyRespectHover = 0;
  communicationHover = 0;

  constructor(
    private fb: FormBuilder,
    private tenantRatingService: TenantRatingService,
    private rentalService: RentalService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {
    this.ratingForm = this.fb.group({
      rentalId: ['', [Validators.required]],
      paymentRating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      propertyRespectRating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      communicationRating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: [''],
      ratingPeriod: ['']
    });
  }

  ngOnInit(): void {
    this.ratingId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.ratingId;

    this.loadRentals();

    if (this.isEditMode && this.ratingId) {
      this.loadRating(this.ratingId);
    }
  }

  loadRentals(): void {
    this.loadingRentals = true;
    this.rentalService.getRentals().subscribe({
      next: (rentals) => {
        // Filter only rentals with tenants
        this.rentals = rentals.filter(r => r.tenantId);
        this.loadingRentals = false;
      },
      error: () => {
        this.loadingRentals = false;
      }
    });
  }

  loadRating(id: string): void {
    this.loading = true;
    this.tenantRatingService.getRating(id).subscribe({
      next: (rating) => {
        this.ratingForm.patchValue({
          rentalId: rating.rentalId || '',
          paymentRating: rating.paymentRating,
          propertyRespectRating: rating.propertyRespectRating,
          communicationRating: rating.communicationRating,
          comment: rating.comment || '',
          ratingPeriod: rating.ratingPeriod || ''
        });
        this.paymentStars = rating.paymentRating;
        this.propertyRespectStars = rating.propertyRespectRating;
        this.communicationStars = rating.communicationRating;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement de l\'évaluation';
        this.loading = false;
      }
    });
  }

  setRating(criteria: string, value: number): void {
    switch (criteria) {
      case 'paymentRating':
        this.paymentStars = value;
        break;
      case 'propertyRespectRating':
        this.propertyRespectStars = value;
        break;
      case 'communicationRating':
        this.communicationStars = value;
        break;
    }
    this.ratingForm.get(criteria)?.setValue(value);
  }

  setHover(criteria: string, value: number): void {
    switch (criteria) {
      case 'paymentRating':
        this.paymentHover = value;
        break;
      case 'propertyRespectRating':
        this.propertyRespectHover = value;
        break;
      case 'communicationRating':
        this.communicationHover = value;
        break;
    }
  }

  getStarValue(criteria: string): number {
    switch (criteria) {
      case 'paymentRating': return this.paymentStars;
      case 'propertyRespectRating': return this.propertyRespectStars;
      case 'communicationRating': return this.communicationStars;
      default: return 0;
    }
  }

  getHoverValue(criteria: string): number {
    switch (criteria) {
      case 'paymentRating': return this.paymentHover;
      case 'propertyRespectRating': return this.propertyRespectHover;
      case 'communicationRating': return this.communicationHover;
      default: return 0;
    }
  }

  getStars(): number[] {
    return [1, 2, 3, 4, 5];
  }

  getSelectedRental(): Rental | undefined {
    const rentalId = this.ratingForm.get('rentalId')?.value;
    return this.rentals.find(r => r.id === rentalId);
  }

  onSubmit(): void {
    if (this.ratingForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const formValue = this.ratingForm.value;
    const selectedRental = this.getSelectedRental();

    if (this.isEditMode && this.ratingId) {
      const request = {
        paymentRating: formValue.paymentRating,
        propertyRespectRating: formValue.propertyRespectRating,
        communicationRating: formValue.communicationRating,
        comment: formValue.comment || undefined,
        ratingPeriod: formValue.ratingPeriod || undefined
      };

      this.tenantRatingService.updateRating(this.ratingId, request).subscribe({
        next: () => {
          this.notificationService.success('Évaluation mise à jour avec succès');
          this.router.navigate(['/ratings']);
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour';
          this.loading = false;
        }
      });
    } else {
      const request = {
        tenantId: selectedRental!.tenantId!,
        rentalId: formValue.rentalId || undefined,
        paymentRating: formValue.paymentRating,
        propertyRespectRating: formValue.propertyRespectRating,
        communicationRating: formValue.communicationRating,
        comment: formValue.comment || undefined,
        ratingPeriod: formValue.ratingPeriod || undefined
      };

      this.tenantRatingService.createRating(request).subscribe({
        next: () => {
          this.notificationService.success('Évaluation créée avec succès');
          this.router.navigate(['/ratings']);
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la création';
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/ratings']);
  }
}
