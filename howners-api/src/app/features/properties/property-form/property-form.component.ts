import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PropertyService } from '../property.service';
import { PropertyType, PROPERTY_TYPE_LABELS, HeatingType, HEATING_TYPE_LABELS, PropertyCondition, PROPERTY_CONDITION_LABELS } from '../../../core/models/property.model';
import { COUNTRIES, Department, getDepartmentsByCountry, getDepartmentFromPostalCode, getDepartmentLabel } from '../../../core/data/geo-reference';
import { PREDEFINED_AMENITIES } from '../../../core/models/listing-amenities.model';

@Component({
  selector: 'app-property-form',
  templateUrl: './property-form.component.html',
  styleUrls: ['./property-form.component.scss']
})
export class PropertyFormComponent implements OnInit {
  propertyForm: FormGroup;
  loading = false;
  error: string | null = null;
  propertyId: string | null = null;
  isEditMode = false;

  propertyTypes = Object.keys(PropertyType).map(key => ({
    value: PropertyType[key as keyof typeof PropertyType],
    label: PROPERTY_TYPE_LABELS[PropertyType[key as keyof typeof PropertyType]]
  }));

  heatingTypes = Object.keys(HeatingType).map(key => ({
    value: HeatingType[key as keyof typeof HeatingType],
    label: HEATING_TYPE_LABELS[HeatingType[key as keyof typeof HeatingType]]
  }));

  propertyConditions = Object.keys(PropertyCondition).map(key => ({
    value: PropertyCondition[key as keyof typeof PropertyCondition],
    label: PROPERTY_CONDITION_LABELS[PropertyCondition[key as keyof typeof PropertyCondition]]
  }));

  dpeGesRatings = ['A', 'B', 'C', 'D', 'E', 'F', 'G'];

  // Équipements du bien. Parking, ascenseur et chauffage collectif sont déjà couverts
  // par des champs dédiés (hasParking, hasElevator, heatingType) — exclus d'ici pour
  // éviter les doublons ; ils sont fusionnés au pré-cochage des annonces.
  propertyAmenities = PREDEFINED_AMENITIES.filter(
    a => !['parking', 'ascenseur', 'chauffage_collectif'].includes(a.key)
  );
  selectedAmenities = new Set<string>();

  countries = COUNTRIES;
  filteredDepartments: Department[] = [];
  getDepartmentLabel = getDepartmentLabel;

  constructor(
    private fb: FormBuilder,
    private propertyService: PropertyService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.propertyForm = this.fb.group({
      name: ['', [Validators.required]],
      propertyType: [PropertyType.APARTMENT, [Validators.required]],
      addressLine1: ['', [Validators.required]],
      addressLine2: [''],
      city: ['', [Validators.required]],
      postalCode: ['', [Validators.required]],
      department: [''],
      country: ['FR'],
      surfaceArea: [null, [Validators.min(0)]],
      bedrooms: [null, [Validators.min(0)]],
      bathrooms: [null, [Validators.min(0)]],
      description: [''],
      condoFees: [null, [Validators.min(0)]],
      propertyTax: [null, [Validators.min(0)]],
      businessTax: [null, [Validators.min(0)]],
      homeInsurance: [null, [Validators.min(0)]],
      purchasePrice: [null, [Validators.min(0)]],
      acquisitionDate: [null],
      landValue: [null, [Validators.min(0)]],
      notaryFees: [null, [Validators.min(0)]],
      dpeRating: [null],
      dpeDate: [null],
      gesRating: [null],
      constructionYear: [null, [Validators.min(1800)]],
      floorNumber: [null],
      totalFloors: [null, [Validators.min(1)]],
      heatingType: [null],
      hasParking: [false],
      hasElevator: [false],
      isFurnished: [false],
      propertyCondition: [null]
    });
  }

  ngOnInit(): void {
    this.propertyId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.propertyId;
    this.updateDepartments();

    if (this.isEditMode && this.propertyId) {
      this.loadProperty(this.propertyId);
    }
  }

  onCountryChange(): void {
    this.updateDepartments();
    this.propertyForm.patchValue({ department: '' });
  }

  onPostalCodeChange(): void {
    const country = this.propertyForm.get('country')?.value;
    const postalCode = this.propertyForm.get('postalCode')?.value;
    if (country === 'FR' && postalCode && postalCode.length >= 2) {
      const dept = getDepartmentFromPostalCode(postalCode, country);
      if (dept) {
        this.propertyForm.patchValue({ department: dept });
      }
    }
  }

  private updateDepartments(): void {
    const country = this.propertyForm.get('country')?.value || 'FR';
    this.filteredDepartments = getDepartmentsByCountry(country);
  }

  loadProperty(id: string): void {
    this.loading = true;
    this.propertyService.getProperty(id).subscribe({
      next: (property) => {
        this.propertyForm.patchValue({
          name: property.name,
          propertyType: property.propertyType,
          addressLine1: property.address.addressLine1,
          addressLine2: property.address.addressLine2,
          city: property.address.city,
          postalCode: property.address.postalCode,
          department: property.address.department || '',
          country: property.address.country,
          surfaceArea: property.surfaceArea,
          bedrooms: property.bedrooms,
          bathrooms: property.bathrooms,
          description: property.description,
          condoFees: property.condoFees,
          propertyTax: property.propertyTax,
          businessTax: property.businessTax,
          homeInsurance: property.homeInsurance,
          purchasePrice: property.purchasePrice,
          acquisitionDate: property.acquisitionDate,
          landValue: property.landValue,
          notaryFees: property.notaryFees,
          dpeRating: property.dpeRating,
          dpeDate: property.dpeDate,
          gesRating: property.gesRating,
          constructionYear: property.constructionYear,
          floorNumber: property.floorNumber,
          totalFloors: property.totalFloors,
          heatingType: property.heatingType,
          hasParking: property.hasParking,
          hasElevator: property.hasElevator,
          isFurnished: property.isFurnished,
          propertyCondition: property.propertyCondition
        });
        this.selectedAmenities = new Set(property.amenities || []);
        this.updateDepartments();
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du bien';
        this.loading = false;
      }
    });
  }

  toggleAmenity(key: string): void {
    if (this.selectedAmenities.has(key)) {
      this.selectedAmenities.delete(key);
    } else {
      this.selectedAmenities.add(key);
    }
  }

  onSubmit(): void {
    if (this.propertyForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const formValue = this.propertyForm.value;
    const request = {
      name: formValue.name,
      propertyType: formValue.propertyType,
      address: {
        addressLine1: formValue.addressLine1,
        addressLine2: formValue.addressLine2,
        city: formValue.city,
        postalCode: formValue.postalCode,
        department: formValue.department || null,
        country: formValue.country || 'FR'
      },
      surfaceArea: formValue.surfaceArea,
      bedrooms: formValue.bedrooms,
      bathrooms: formValue.bathrooms,
      description: formValue.description,
      condoFees: formValue.condoFees,
      propertyTax: formValue.propertyTax,
      businessTax: formValue.businessTax,
      homeInsurance: formValue.homeInsurance,
      purchasePrice: formValue.purchasePrice,
      acquisitionDate: formValue.acquisitionDate || null,
      landValue: formValue.landValue,
      notaryFees: formValue.notaryFees,
      dpeRating: formValue.dpeRating,
      dpeDate: formValue.dpeDate || null,
      gesRating: formValue.gesRating,
      constructionYear: formValue.constructionYear,
      floorNumber: formValue.floorNumber,
      totalFloors: formValue.totalFloors,
      heatingType: formValue.heatingType,
      hasParking: formValue.hasParking,
      hasElevator: formValue.hasElevator,
      isFurnished: formValue.isFurnished,
      amenities: Array.from(this.selectedAmenities),
      propertyCondition: formValue.propertyCondition
    };

    const operation = this.isEditMode && this.propertyId
      ? this.propertyService.updateProperty(this.propertyId, request)
      : this.propertyService.createProperty(request);

    operation.subscribe({
      next: (saved) => {
        // Après CRÉATION : atterrir sur la fiche du bien, où le bandeau « publiez votre
        // annonce » guide l'étape suivante. Après édition : retour à la liste.
        if (this.isEditMode) {
          this.router.navigate(['/properties']);
        } else {
          this.router.navigate(['/properties', saved.id]);
        }
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de l\'enregistrement';
        this.loading = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/properties']);
  }
}
