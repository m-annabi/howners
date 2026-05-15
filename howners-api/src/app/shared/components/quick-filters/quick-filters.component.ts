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

  select(key: string): void {
    if (key !== this.activeKey) {
      this.filterChange.emit(key);
    }
  }
}
