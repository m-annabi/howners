import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TenantSearchProfileService } from '../../../core/services/tenant-search-profile.service';
import { TenantSearchProfile, FurnishedPreference, FURNISHED_PREFERENCE_LABELS, CreateTenantSearchProfileRequest } from '../../../core/models/tenant-search-profile.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { Department, getDepartmentsByCountry, getDepartmentLabel } from '../../../core/data/geo-reference';

@Component({
  selector: 'app-search-profile-form',
  templateUrl: './search-profile-form.component.html',
  styleUrls: ['./search-profile-form.component.scss']
})
export class SearchProfileFormComponent implements OnInit {
  form!: FormGroup;
  profile: TenantSearchProfile | null = null;
  loading = false;
  saving = false;
  success = false;
  error = '';

  departments: Department[] = [];
  getDepartmentLabel = getDepartmentLabel;
  propertyTypes = Object.values(PropertyType);
  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  furnishedPreferences = Object.values(FurnishedPreference);
  furnishedLabels = FURNISHED_PREFERENCE_LABELS;

  constructor(
    private fb: FormBuilder,
    private profileService: TenantSearchProfileService
  ) {}

  ngOnInit(): void {
    this.departments = getDepartmentsByCountry('FR');
    this.initForm();
    this.loadProfile();
  }

  private initForm(): void {
    this.form = this.fb.group({
      desiredDepartment: [''],
      desiredCity: [''],
      desiredPostalCode: [''],
      budgetMin: [null],
      budgetMax: [null],
      desiredPropertyType: [''],
      minSurface: [null],
      minBedrooms: [null],
      furnishedPreference: [FurnishedPreference.NO_PREFERENCE],
      desiredMoveIn: [''],
      description: ['']
    });
  }

  private loadProfile(): void {
    this.loading = true;
    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.form.patchValue({
          desiredDepartment: profile.desiredDepartment || '',
          desiredCity: profile.desiredCity || '',
          desiredPostalCode: profile.desiredPostalCode || '',
          budgetMin: profile.budgetMin,
          budgetMax: profile.budgetMax,
          desiredPropertyType: profile.desiredPropertyType || '',
          minSurface: profile.minSurface,
          minBedrooms: profile.minBedrooms,
          furnishedPreference: profile.furnishedPreference || FurnishedPreference.NO_PREFERENCE,
          desiredMoveIn: profile.desiredMoveIn || '',
          description: profile.description || ''
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  save(): void {
    this.saving = true;
    this.success = false;
    this.error = '';

    const val = this.form.value;
    const request: CreateTenantSearchProfileRequest = {
      desiredCity: val.desiredCity || undefined,
      desiredDepartment: val.desiredDepartment || undefined,
      desiredPostalCode: val.desiredPostalCode || undefined,
      budgetMin: val.budgetMin || undefined,
      budgetMax: val.budgetMax || undefined,
      desiredPropertyType: val.desiredPropertyType || undefined,
      minSurface: val.minSurface || undefined,
      minBedrooms: val.minBedrooms || undefined,
      furnishedPreference: val.furnishedPreference || FurnishedPreference.NO_PREFERENCE,
      desiredMoveIn: val.desiredMoveIn || undefined,
      description: val.description || undefined
    };

    this.profileService.createOrUpdate(request).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.saving = false;
        this.success = true;
        setTimeout(() => this.success = false, 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error = 'Erreur lors de la sauvegarde du profil.';
      }
    });
  }

  toggleActive(): void {
    if (!this.profile) return;
    const action = this.profile.isActive
      ? this.profileService.deactivate()
      : this.profileService.activate();

    action.subscribe({
      next: (profile) => {
        this.profile = profile;
      },
      error: () => {
        this.error = 'Erreur lors du changement de statut.';
      }
    });
  }
}
