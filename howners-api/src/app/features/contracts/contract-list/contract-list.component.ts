import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContractService } from '../../../core/services/contract.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  Contract,
  ContractStatus,
  CONTRACT_STATUS_LABELS,
  CONTRACT_STATUS_COLORS
} from '../../../core/models/contract.model';
import { UsageLimits } from '../../../core/models/subscription.model';
import { QuickFilter } from '../../../shared/components/quick-filters/quick-filters.component';

@Component({
  selector: 'app-contract-list',
  templateUrl: './contract-list.component.html',
  styleUrls: ['./contract-list.component.css']
})
export class ContractListComponent implements OnInit {
  contracts: Contract[] = [];
  filteredContracts: Contract[] = [];
  filters: QuickFilter[] = [];
  loading = false;
  error: string | null = null;
  searchTerm = '';
  selectedStatus: string = 'ALL';
  sortCol = '';
  sortDir: 'asc' | 'desc' = 'desc';

  ContractStatus = ContractStatus;
  contractStatusLabels = CONTRACT_STATUS_LABELS;
  contractStatusColors = CONTRACT_STATUS_COLORS;

  usage: UsageLimits | null = null;
  showUpgradeModal = false;
  isOwner = false;

  constructor(
    private contractService: ContractService,
    private subscriptionService: SubscriptionService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    public authService: AuthService
  ) {}

  get quotaLabel(): string {
    if (!this.usage) return '';
    if (this.usage.maxContractsPerMonth < 0) {
      return `${this.usage.currentContractsThisMonth} contrats ce mois · illimité`;
    }
    return `${this.usage.currentContractsThisMonth} / ${this.usage.maxContractsPerMonth} contrats ce mois`;
  }

  get quotaPercent(): number {
    if (!this.usage || this.usage.maxContractsPerMonth <= 0) return 0;
    return Math.min(100, (this.usage.currentContractsThisMonth / this.usage.maxContractsPerMonth) * 100);
  }

  get quotaTone(): 'ok' | 'warning' | 'danger' {
    const pct = this.quotaPercent;
    if (pct >= 100) return 'danger';
    if (pct >= 80) return 'warning';
    return 'ok';
  }

  loadUsage(): void {
    this.subscriptionService.getUsageLimits().subscribe({
      next: (u) => this.usage = u,
      error: () => {}
    });
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      const wasOwner = this.isOwner;
      this.isOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';
      if (this.isOwner && !wasOwner) this.loadUsage();
    });

    this.route.queryParams.subscribe(params => {
      const f = params['filter'];
      if (f === 'sent') this.selectedStatus = ContractStatus.SENT;
      else if (f === 'expiring') this.selectedStatus = 'EXPIRING';
      else if (Object.values(ContractStatus).includes(f)) this.selectedStatus = f;
    });
    this.loadContracts();
  }

  sortIcon(col: string): string {
    if (this.sortCol !== col) return 'bi-arrow-down-up';
    return this.sortDir === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down';
  }

  sortOn(col: string): void {
    this.sortDir = this.sortCol === col && this.sortDir === 'desc' ? 'asc' : 'desc';
    this.sortCol = col;
    this.applyFilters();
  }

  private buildFilters(): void {
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
    this.filters = list.filter(f => f.key === 'ALL' || (f.count || 0) > 0);
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
        this.buildFilters();
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

    if (this.sortCol) {
      filtered = filtered.slice().sort((a, b) => {
        let diff = 0;
        if (this.sortCol === 'date') diff = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        else if (this.sortCol === 'tenant') diff = (a.tenantFullName || '').localeCompare(b.tenantFullName || '');
        else if (this.sortCol === 'property') diff = a.rentalPropertyName.localeCompare(b.rentalPropertyName);
        else if (this.sortCol === 'status') diff = a.status.localeCompare(b.status);
        return this.sortDir === 'asc' ? diff : -diff;
      });
    } else {
      filtered = filtered.slice().sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
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
    if (this.isOwner && this.usage && !this.usage.canCreateContract) {
      this.showUpgradeModal = true;
      return;
    }
    this.router.navigate(['/contracts/new']);
  }

  closeUpgradeModal(): void {
    this.showUpgradeModal = false;
  }

  goToPricing(): void {
    this.showUpgradeModal = false;
    this.router.navigate(['/billing/pricing']);
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
