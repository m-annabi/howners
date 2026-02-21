import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import {
  TemplateVariable,
  VARIABLE_CATEGORY_LABELS,
  VARIABLE_CATEGORY_COLORS
} from '../../../core/models/contract-template.model';

@Component({
  selector: 'app-variable-helper',
  templateUrl: './variable-helper.component.html',
  styleUrls: ['./variable-helper.component.css']
})
export class VariableHelperComponent implements OnInit {
  @Output() variableSelected = new EventEmitter<string>();

  variables: TemplateVariable[] = [];
  filteredVariables: TemplateVariable[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedCategory = 'all';

  categories = ['all', 'owner', 'tenant', 'property', 'rental', 'date'];
  categoryLabels = VARIABLE_CATEGORY_LABELS;
  categoryColors = VARIABLE_CATEGORY_COLORS;

  constructor(private templateService: ContractTemplateService) {}

  ngOnInit(): void {
    this.loadVariables();
  }

  loadVariables(): void {
    this.loading = true;
    this.error = null;

    this.templateService.getAvailableVariables().subscribe({
      next: (response) => {
        this.variables = response.variables;
        this.filteredVariables = response.variables;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading variables:', err);
        this.error = 'Erreur lors du chargement des variables';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.variables;

    // Filtre par catégorie
    if (this.selectedCategory !== 'all') {
      filtered = filtered.filter(v => v.category === this.selectedCategory);
    }

    // Filtre par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(v =>
        v.key.toLowerCase().includes(term) ||
        v.label.toLowerCase().includes(term)
      );
    }

    this.filteredVariables = filtered;
  }

  onCategoryChange(): void {
    this.applyFilters();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  insertVariable(variable: TemplateVariable): void {
    this.variableSelected.emit(`{{${variable.key}}}`);
  }

  getCategoryLabel(category: string): string {
    return this.categoryLabels[category] || category;
  }

  getCategoryColor(category: string): string {
    return this.categoryColors[category] || '#6c757d';
  }
}
