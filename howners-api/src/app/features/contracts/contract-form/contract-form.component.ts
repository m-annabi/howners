import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ContractService } from '../../../core/services/contract.service';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import { RentalService } from '../../rentals/rental.service';
import { CreateContractRequest } from '../../../core/models/contract.model';
import { Rental } from '../../../core/models/rental.model';
import { ContractTemplate, RentalType, RENTAL_TYPE_LABELS } from '../../../core/models/contract-template.model';

@Component({
  selector: 'app-contract-form',
  templateUrl: './contract-form.component.html',
  styleUrls: ['./contract-form.component.css']
})
export class ContractFormComponent implements OnInit {
  contractForm: FormGroup;
  rentals: Rental[] = [];
  templates: ContractTemplate[] = [];
  loading = false;
  submitting = false;
  error: string | null = null;
  creationMode: 'quick' | 'customize' | null = null;

  constructor(
    private fb: FormBuilder,
    private contractService: ContractService,
    private templateService: ContractTemplateService,
    private rentalService: RentalService,
    private router: Router
  ) {
    this.contractForm = this.fb.group({
      rentalId: ['', Validators.required],
      templateId: [null]  // Optionnel, null utilise le template par défaut
    });
  }

  ngOnInit(): void {
    this.loadRentals();
    this.loadTemplates();
  }

  loadRentals(): void {
    this.loading = true;
    this.error = null;

    this.rentalService.getRentals().subscribe({
      next: (rentals: Rental[]) => {
        // Filtrer uniquement les locations actives ou en attente
        this.rentals = rentals.filter((r: Rental) =>
          r.status === 'ACTIVE' || r.status === 'PENDING'
        );
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading rentals:', err);
        this.error = 'Erreur lors du chargement des locations';
        this.loading = false;
      }
    });
  }

  loadTemplates(): void {
    this.templateService.getMyTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
      },
      error: (err) => {
        console.error('Error loading templates:', err);
        // Ne pas bloquer si le chargement des templates échoue
      }
    });
  }

  getFilteredTemplates(): ContractTemplate[] {
    const selectedRental = this.rentals.find(r => r.id === this.contractForm.value.rentalId);
    if (!selectedRental) {
      return this.templates;
    }

    // Filtrer les templates selon le type de location
    const rentalType = selectedRental.rentalType as RentalType;
    return this.templates.filter(t => t.rentalType === rentalType);
  }

  quickGenerate(): void {
    this.creationMode = 'quick';
    this.onSubmit();
  }

  customizeBeforeGenerate(): void {
    if (this.contractForm.invalid) {
      this.contractForm.markAllAsTouched();
      return;
    }

    this.creationMode = 'customize';
    this.router.navigate(['/contracts/customize'], {
      queryParams: {
        rentalId: this.contractForm.value.rentalId,
        templateId: this.contractForm.value.templateId
      }
    });
  }

  onSubmit(): void {
    if (this.contractForm.invalid) {
      this.contractForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.error = null;

    const request: CreateContractRequest = {
      rentalId: this.contractForm.value.rentalId,
      templateId: this.contractForm.value.templateId
    };

    this.contractService.createContract(request).subscribe({
      next: (contract) => {
        this.submitting = false;
        this.router.navigate(['/contracts', contract.id]);
      },
      error: (err) => {
        console.error('Error creating contract:', err);
        this.error = err.error?.message || 'Erreur lors de la création du contrat';
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/contracts']);
  }

  get rentalId() {
    return this.contractForm.get('rentalId');
  }

  get isTemplateSelected(): boolean {
    const templateId = this.contractForm.value.templateId;
    return templateId !== null && templateId !== 'null' && templateId !== '';
  }
}
