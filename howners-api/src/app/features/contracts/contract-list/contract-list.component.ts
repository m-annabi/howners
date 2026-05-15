import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Contract,
  ContractStatus,
  CONTRACT_STATUS_LABELS,
  CONTRACT_STATUS_COLORS
} from '../../../core/models/contract.model';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

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
  selectedStatus: string = 'ALL';

  ContractStatus = ContractStatus;
  contractStatusLabels = CONTRACT_STATUS_LABELS;
  contractStatusColors = CONTRACT_STATUS_COLORS;

  constructor(
    private contractService: ContractService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const f = params['filter'];
      if (f === 'sent') this.selectedStatus = ContractStatus.SENT;
      else if (f === 'expiring') this.selectedStatus = 'EXPIRING';
      else if (Object.values(ContractStatus).includes(f)) this.selectedStatus = f;
    });
    this.loadContracts();
  }

  get filters(): QuickFilter[] {
    const counts = new Map<string, number>();
    counts.set('ALL', this.contracts.length);
    for (const c of this.contracts) {
      counts.set(c.status, (counts.get(c.status) || 0) + 1);
    }

    const expiring = this.countExpiring();

    const list: QuickFilter[] = [
      { key: 'ALL', label: 'Tous', count: counts.get('ALL') || 0 },
      { key: ContractStatus.DRAFT, label: 'Brouillon', count: counts.get(ContractStatus.DRAFT) || 0 },
      { key: ContractStatus.SENT, label: 'Envoyés', count: counts.get(ContractStatus.SENT) || 0, tone: 'warning' },
      { key: ContractStatus.SIGNED, label: 'Signés', count: counts.get(ContractStatus.SIGNED) || 0, tone: 'success' },
      { key: ContractStatus.ACTIVE, label: 'Actifs', count: counts.get(ContractStatus.ACTIVE) || 0, tone: 'success' },
      { key: 'EXPIRING', label: 'Expire ≤ 30 j', count: expiring, tone: 'danger' },
      { key: ContractStatus.TERMINATED, label: 'Terminés', count: counts.get(ContractStatus.TERMINATED) || 0 }
    ];
    return list.filter(f => f.key === 'ALL' || (f.count || 0) > 0);
  }

  private countExpiring(): number {
    const today = new Date();
    const in30Days = new Date();
    in30Days.setDate(today.getDate() + 30);
    return this.contracts.filter(c => {
      if (c.status !== ContractStatus.ACTIVE && c.status !== ContractStatus.SIGNED) return false;
      const end = (c as any).rentalEndDate || (c as any).endDate;
      if (!end) return false;
      const endDate = new Date(end);
      return endDate >= today && endDate <= in30Days;
    }).length;
  }

  loadContracts(): void {
    this.loading = true;
    this.error = null;

    this.contractService.getMyContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des contrats';
        this.loading = false;
      }
    });
  }

  onFilterChange(key: string): void {
    this.selectedStatus = key;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = this.contracts;

    if (this.selectedStatus === 'EXPIRING') {
      const today = new Date();
      const in30Days = new Date();
      in30Days.setDate(today.getDate() + 30);
      filtered = filtered.filter(c => {
        if (c.status !== ContractStatus.ACTIVE && c.status !== ContractStatus.SIGNED) return false;
        const end = (c as any).rentalEndDate || (c as any).endDate;
        if (!end) return false;
        const endDate = new Date(end);
        return endDate >= today && endDate <= in30Days;
      });
    } else if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(c => c.status === this.selectedStatus);
    }

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
        next: () => this.loadContracts(),
        error: () => {
          this.notificationService.error('Erreur lors de la suppression du contrat');
        }
      });
    }
  }
}
