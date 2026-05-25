import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface QuickFilter {
  key: string;
  label: string;
  count?: number;
  tone?: 'neutral' | 'warning' | 'danger' | 'success';
}

@Component({
  selector: 'app-quick-filters',
  templateUrl: './quick-filters.component.html',
  styleUrls: ['./quick-filters.component.scss']
})
export class QuickFiltersComponent {
  @Input() filters: QuickFilter[] = [];
  @Input() activeKey = '';
  @Output() filterChange = new EventEmitter<string>();

  trackFilter(_: number, f: QuickFilter): string { return f.key; }

  select(key: string): void {
    this.filterChange.emit(key);
  }
}
