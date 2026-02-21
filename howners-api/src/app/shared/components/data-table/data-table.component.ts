import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

export interface DataTableColumn {
  key: string;
  label: string;
  type?: 'text' | 'date' | 'currency' | 'badge' | 'actions';
  sortable?: boolean;
  width?: string;
  badgeType?: string;
}

@Component({
  selector: 'app-data-table',
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss']
})
export class DataTableComponent implements OnChanges {
  @Input() columns: DataTableColumn[] = [];
  @Input() data: any[] = [];
  @Input() loading = false;
  @Input() emptyIcon = 'bi-inbox';
  @Input() emptyMessage = 'Aucun élément trouvé';
  @Input() searchable = true;
  @Input() pageSize = 10;

  @Output() rowClick = new EventEmitter<any>();
  @Output() action = new EventEmitter<{ action: string; row: any }>();

  searchTerm = '';
  currentPage = 1;
  sortKey = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  filteredData: any[] = [];
  pagedData: any[] = [];
  totalPages = 1;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] || changes['pageSize']) {
      this.applyFilters();
    }
  }

  onSearch(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  onSort(col: DataTableColumn): void {
    if (!col.sortable) return;
    if (this.sortKey === col.key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = col.key;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.applyPagination();
  }

  onRowClick(row: any): void {
    this.rowClick.emit(row);
  }

  onAction(actionName: string, row: any, event: Event): void {
    event.stopPropagation();
    this.action.emit({ action: actionName, row });
  }

  getValue(row: any, key: string): any {
    return key.split('.').reduce((obj, k) => obj?.[k], row);
  }

  get pages(): number[] {
    const range: number[] = [];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 4);
    for (let i = start; i <= end; i++) {
      range.push(i);
    }
    return range;
  }

  private applyFilters(): void {
    let result = [...this.data];

    // Search
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(row =>
        this.columns.some(col => {
          const val = this.getValue(row, col.key);
          return val != null && String(val).toLowerCase().includes(term);
        })
      );
    }

    // Sort
    if (this.sortKey) {
      result.sort((a, b) => {
        const aVal = this.getValue(a, this.sortKey);
        const bVal = this.getValue(b, this.sortKey);
        let cmp = 0;
        if (aVal < bVal) cmp = -1;
        else if (aVal > bVal) cmp = 1;
        return this.sortDirection === 'asc' ? cmp : -cmp;
      });
    }

    this.filteredData = result;
    this.totalPages = Math.max(1, Math.ceil(this.filteredData.length / this.pageSize));
    if (this.currentPage > this.totalPages) this.currentPage = 1;
    this.applyPagination();
  }

  private applyPagination(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    this.pagedData = this.filteredData.slice(start, start + this.pageSize);
  }
}
