import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stats-card',
  templateUrl: './stats-card.component.html',
  styleUrls: ['./stats-card.component.scss']
})
export class StatsCardComponent {
  @Input() label = '';
  @Input() value: string | number = 0;
  @Input() icon = 'bi-bar-chart';
  @Input() trend?: number;
  @Input() color: 'primary' | 'success' | 'warning' | 'danger' = 'primary';
  @Input() routerLink?: string;

  get trendIcon(): string {
    if (!this.trend) return '';
    return this.trend > 0 ? 'bi-arrow-up-right' : 'bi-arrow-down-right';
  }

  get trendClass(): string {
    if (!this.trend) return '';
    return this.trend > 0 ? 'trend--up' : 'trend--down';
  }
}
