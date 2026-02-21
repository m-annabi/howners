import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss']
})
export class EmptyStateComponent {
  @Input() icon = 'bi-inbox';
  @Input() title = 'Aucun élément';
  @Input() message = '';
  @Input() actionLabel?: string;
  @Input() actionRoute?: string;
}
