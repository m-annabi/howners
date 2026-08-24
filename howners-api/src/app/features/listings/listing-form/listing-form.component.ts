import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ListingService } from '../../../core/services/listing.service';
import { ListingPhotoService } from '../../../core/services/listing-photo.service';
import { PropertyService } from '../../properties/property.service';
import { Property, HeatingType } from '../../../core/models/property.model';
import { ListingPhoto } from '../../../core/models/listing.model';
import { ListingPhotoUploadComponent } from '../../../shared/components/listing-photo-upload/listing-photo-upload.component';
import {
  PREDEFINED_AMENITIES,
  PREDEFINED_REQUIREMENTS,
  AmenityItem
} from '../../../core/models/listing-amenities.model';

@Component({
  selector: 'app-listing-form',
  templateUrl: './listing-form.component.html',
  styleUrls: ['./listing-form.component.scss']
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
  /** Équipements pré-cochés automatiquement d'après le bien sélectionné (retirables). */
  private autoAmenities = new Set<string>();

  // Photos
  listingPhotos: ListingPhoto[] = [];
  listingStatus: string | null = null;

  @ViewChild(ListingPhotoUploadComponent) photoUploader?: ListingPhotoUploadComponent;

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
      next: (page) => {
        this.properties = page.content;
        // Si un bien est déjà sélectionné (chargement direct), pré-coche ses équipements.
        const current = this.form.get('propertyId')?.value;
        if (!this.isEditMode && current) this.deriveAmenitiesFromProperty(current);
      },
      error: () => {} // silent — dropdown stays empty
    });

    // À la sélection d'un bien, pré-cocher les équipements qu'il déclare. En édition,
    // le patch initial est fait sans émettre d'événement pour ne pas écraser les
    // équipements déjà enregistrés sur l'annonce.
    this.form.get('propertyId')!.valueChanges.subscribe(id => {
      if (id) this.deriveAmenitiesFromProperty(id);
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
          }, { emitEvent: false }); // ne pas re-dériver : on garde les équipements enregistrés

          this.listingStatus = listing.status;

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

  /**
   * Pré-coche les équipements déductibles du bien sélectionné (parking, ascenseur,
   * chauffage collectif). Les équipements déduits d'un bien précédemment choisi sont
   * d'abord retirés ; l'utilisateur reste libre de décocher ou d'ajouter les autres.
   */
  private deriveAmenitiesFromProperty(propertyId: string): void {
    const property = this.properties.find(p => p.id === propertyId);
    if (!property) return;

    // Retire les équipements auto-déduits du bien précédent (sans toucher aux choix manuels).
    this.autoAmenities.forEach(k => this.selectedAmenities.delete(k));

    const derived = new Set<string>();
    if (property.hasParking) derived.add('parking');
    if (property.hasElevator) derived.add('ascenseur');
    if (property.heatingType === HeatingType.COLLECTIVE_GAS
        || property.heatingType === HeatingType.COLLECTIVE_ELECTRIC
        || property.heatingType === HeatingType.DISTRICT_HEATING) {
      derived.add('chauffage_collectif');
    }

    derived.forEach(k => this.selectedAmenities.add(k));
    this.autoAmenities = derived;
  }

  /** Vrai si l'équipement a été pré-coché automatiquement d'après le bien. */
  isAutoAmenity(key: string): boolean {
    return this.autoAmenities.has(key) && this.selectedAmenities.has(key);
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
      error: () => {}
    });
  }

  async onSubmit(publish = false): Promise<void> {
    if (this.form.invalid) return;

    this.submitting = true;

    // Les photos sélectionnées mais pas encore téléversées partent avec la
    // sauvegarde : plus besoin de cliquer un second bouton dans le composant.
    if (this.isEditMode && this.photoUploader && this.photoUploader.selectedFiles.length > 0) {
      await this.photoUploader.uploadAll();
    }

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
        if (publish && String(listing.status) !== 'PUBLISHED') {
          this.listingService.publishListing(listing.id).subscribe({
            next: () => {
              this.submitting = false;
              this.router.navigate(['/listings', listing.id]);
            },
            error: () => {
              this.submitting = false;
              this.router.navigate(['/listings', listing.id]);
            }
          });
        } else {
          this.submitting = false;
          this.router.navigate(['/listings', listing.id]);
        }
      },
      error: () => this.submitting = false
    });
  }
}
