import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Contract,
  ContractStatus,
  CONTRACT_STATUS_LABELS,
  CONTRACT_STATUS_COLORS
} from '../../../core/models/contract.model';

@Component({
  selector: 'app-contract-list',
  templateUrl: './contract-list.component.html',
  styleUrls: ['./contract-list.component.css']
})
export class ContractListComponent implements OnInit {
  contracts: Contract[] = [];
  filteredContracts: Contract[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedStatus: ContractStatus | 'ALL' = 'ALL';

  // Enums et constantes pour le template
  ContractStatus = ContractStatus;
  contractStatusLabels = CONTRACT_STATUS_LABELS;
  contractStatusColors = CONTRACT_STATUS_COLORS;

  statuses = [
    { value: 'ALL', label: 'Tous les statuts' },
    ...Object.keys(ContractStatus).map(key => ({
      value: ContractStatus[key as keyof typeof ContractStatus],
      label: CONTRACT_STATUS_LABELS[ContractStatus[key as keyof typeof ContractStatus]]
    }))
  ];

  constructor(
    private contractService: ContractService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    this.loading = true;
    this.error = null;

    this.contractService.getMyContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.filteredContracts = contracts;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading contracts:', err);
        this.error = 'Erreur lors du chargement des contrats';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.contracts;

    // Filtre par statut
    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(contract => contract.status === this.selectedStatus);
    }

    // Filtre par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(contract =>
        contract.contractNumber.toLowerCase().includes(term) ||
        contract.rentalPropertyName.toLowerCase().includes(term) ||
        contract.tenantFullName.toLowerCase().includes(term)
      );
    }

    this.filteredContracts = filtered;
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getStatusColor(status: ContractStatus): string {
    return this.contractStatusColors[status];
  }

  getStatusLabel(status: ContractStatus): string {
    return this.contractStatusLabels[status];
  }

  viewContract(contract: Contract): void {
    this.router.navigate(['/contracts', contract.id]);
  }

  createContract(): void {
    this.router.navigate(['/contracts/new']);
  }

  deleteContract(contract: Contract, event: Event): void {
    event.stopPropagation();

    if (contract.status !== ContractStatus.DRAFT) {
      this.notificationService.success('Seuls les contrats en brouillon peuvent être supprimés');
      return;
    }

    if (confirm(`Êtes-vous sûr de vouloir supprimer le contrat ${contract.contractNumber} ?`)) {
      this.contractService.deleteContract(contract.id).subscribe({
        next: () => {
          this.loadContracts();
        },
        error: (err) => {
          console.error('Error deleting contract:', err);
          this.notificationService.error('Erreur lors de la suppression du contrat');
        }
      });
    }
  }
}
