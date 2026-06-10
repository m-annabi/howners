import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ListingService } from '../../../core/services/listing.service';
import { ListingPhotoService } from '../../../core/services/listing-photo.service';
import { PropertyService } from '../../properties/property.service';
import { Property } from '../../../core/models/property.model';
import { ListingPhoto } from '../../../core/models/listing.model';
import {
  PREDEFINED_AMENITIES,
  PREDEFINED_REQUIREMENTS,
  AmenityItem
} from '../../../core/models/listing-amenities.model';

@Component({
  selector: 'app-listing-form',
  templateUrl: './listing-form.component.html'
})
export class ListingFormComponent implements OnInit {
  form!: FormGroup;
  properties: Property[] = [];
  isEditMode = false;
  listingId: string | null = null;
  loading = false;
  submitting = false;

  // Amenities & requirements
  predefinedAmenities = PREDEFINED_AMENITIES;
  predefinedRequirements = PREDEFINED_REQUIREMENTS;
  selectedAmenities = new Set<string>();
  selectedRequirements = new Set<string>();

  // Photos
  listingPhotos: ListingPhoto[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private listingService: ListingService,
    private listingPhotoService: ListingPhotoService,
    private propertyService: PropertyService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      propertyId: ['', Validators.required],
      title: ['', Validators.required],
      description: [''],
      pricePerMonth: [null],
      pricePerNight: [null],
      currency: ['EUR'],
      minStay: [null],
      maxStay: [null],
      availableFrom: [null]
    });

    this.listingId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.listingId;

    this.propertyService.getProperties().subscribe({
      next: (properties) => this.properties = properties,
      error: () => {} // silent — dropdown stays empty
    });

    if (this.isEditMode && this.listingId) {
      this.loading = true;
      this.listingService.getListing(this.listingId).subscribe({
        next: (listing) => {
          this.form.patchValue({
            propertyId: listing.propertyId,
            title: listing.title,
            description: listing.description,
            pricePerMonth: listing.pricePerMonth,
            pricePerNight: listing.pricePerNight,
            currency: listing.currency,
            minStay: listing.minStay,
            maxStay: listing.maxStay,
            availableFrom: listing.availableFrom
          });

          // Load amenities as keys
          if (listing.amenities) {
            listing.amenities.forEach(a => this.selectedAmenities.add(a));
          }
          if (listing.requirements) {
            listing.requirements.forEach(r => this.selectedRequirements.add(r));
          }

          // Load photos
          this.listingPhotos = listing.photos || [];

          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.router.navigate(['/listings/my']);
        }
      });
    }
  }

  toggleAmenity(key: string): void {
    if (this.selectedAmenities.has(key)) {
      this.selectedAmenities.delete(key);
    } else {
      this.selectedAmenities.add(key);
    }
  }

  toggleRequirement(key: string): void {
    if (this.selectedRequirements.has(key)) {
      this.selectedRequirements.delete(key);
    } else {
      this.selectedRequirements.add(key);
    }
  }

  onPhotoUploaded(photo: ListingPhoto): void {
    this.listingPhotos.push(photo);
  }

  deletePhoto(photo: ListingPhoto): void {
    if (!this.listingId || !confirm('Voulez-vous vraiment supprimer cette photo ?')) {
      return;
    }

    this.listingPhotoService.deletePhoto(this.listingId, photo.id).subscribe({
      next: () => {
        this.listingPhotos = this.listingPhotos.filter(p => p.id !== photo.id);
      },
      error: (err) => console.error('Error deleting photo:', err)
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.submitting = true;
    const amenitiesArray = Array.from(this.selectedAmenities);
    const requirementsArray = Array.from(this.selectedRequirements);

    const request = {
      ...this.form.value,
      amenities: amenitiesArray.length > 0 ? amenitiesArray : null,
      requirements: requirementsArray.length > 0 ? requirementsArray : null
    };

    const action = this.isEditMode
      ? this.listingService.updateListing(this.listingId!, request)
      : this.listingService.createListing(request);

    action.subscribe({
      next: (listing) => {
        this.submitting = false;
        this.router.navigate(['/listings', listing.id]);
      },
      error: () => this.submitting = false
    });
  }
}
