import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  ContractTemplate,
  RentalType,
  RENTAL_TYPE_LABELS
} from '../../../core/models/contract-template.model';

@Component({
  selector: 'app-template-list',
  templateUrl: './template-list.component.html',
  styleUrls: ['./template-list.component.css']
})
export class TemplateListComponent implements OnInit {
  templates: ContractTemplate[] = [];
  filteredTemplates: ContractTemplate[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedType: RentalType | 'ALL' = 'ALL';

  // Enums et constantes pour le template
  RentalType = RentalType;
  rentalTypeLabels = RENTAL_TYPE_LABELS;

  types = [
    { value: 'ALL', label: 'Tous les types' },
    ...Object.keys(RentalType).map(key => ({
      value: RentalType[key as keyof typeof RentalType],
      label: RENTAL_TYPE_LABELS[RentalType[key as keyof typeof RentalType]]
    }))
  ];

  constructor(
    private templateService: ContractTemplateService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.error = null;

    this.templateService.getMyTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.filteredTemplates = templates;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading templates:', err);
        this.error = 'Erreur lors du chargement des templates';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.templates;

    // Filtre par type de location
    if (this.selectedType !== 'ALL') {
      filtered = filtered.filter(template => template.rentalType === this.selectedType);
    }

    // Filtre par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(template =>
        template.name.toLowerCase().includes(term) ||
        (template.description && template.description.toLowerCase().includes(term))
      );
    }

    this.filteredTemplates = filtered;
  }

  onTypeChange(): void {
    this.applyFilters();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getRentalTypeLabel(type: RentalType): string {
    return this.rentalTypeLabels[type];
  }

  createTemplate(): void {
    this.router.navigate(['/templates/new']);
  }

  editTemplate(template: ContractTemplate, event: Event): void {
    event.stopPropagation();

    if (template.isDefault) {
      this.notificationService.success('Les templates par défaut ne peuvent pas être modifiés');
      return;
    }

    this.router.navigate(['/templates', template.id, 'edit']);
  }

  duplicateTemplate(template: ContractTemplate, event: Event): void {
    event.stopPropagation();

    const newName = prompt(`Nom du template dupliqué :`, `Copie de ${template.name}`);
    if (!newName || !newName.trim()) {
      return;
    }

    this.templateService.duplicateTemplate(template.id, newName.trim()).subscribe({
      next: () => {
        this.loadTemplates();
      },
      error: (err) => {
        console.error('Error duplicating template:', err);
        this.notificationService.error('Erreur lors de la duplication du template');
      }
    });
  }

  deleteTemplate(template: ContractTemplate, event: Event): void {
    event.stopPropagation();

    if (template.isDefault) {
      this.notificationService.success('Les templates par défaut ne peuvent pas être supprimés');
      return;
    }

    if (confirm(`Êtes-vous sûr de vouloir supprimer le template "${template.name}" ?`)) {
      this.templateService.deleteTemplate(template.id).subscribe({
        next: () => {
          this.loadTemplates();
        },
        error: (err) => {
          console.error('Error deleting template:', err);
          this.notificationService.error('Erreur lors de la suppression du template');
        }
      });
    }
  }
}
