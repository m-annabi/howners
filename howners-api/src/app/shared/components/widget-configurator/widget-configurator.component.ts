import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { WidgetConfig, WidgetDefinition } from '../../../core/models/widget-config.model';

@Component({
  selector: 'app-widget-configurator',
  templateUrl: './widget-configurator.component.html',
  styleUrls: ['./widget-configurator.component.scss']
})
export class WidgetConfiguratorComponent implements OnChanges {
  @Input() definitions: WidgetDefinition[] = [];
  @Input() configs: WidgetConfig[] = [];
  @Output() save = new EventEmitter<WidgetConfig[]>();
  @Output() closePanel = new EventEmitter<void>();

  items: WidgetConfig[] = [];

  ngOnChanges(): void {
    this.items = [...this.configs].sort((a, b) => a.order - b.order);
  }

  getDefinition(id: string): WidgetDefinition | undefined {
    return this.definitions.find(d => d.id === id);
  }

  onDrop(event: CdkDragDrop<WidgetConfig[]>): void {
    moveItemInArray(this.items, event.previousIndex, event.currentIndex);
    this.items = this.items.map((item, i) => ({ ...item, order: i }));
  }

  onSave(): void {
    this.save.emit(this.items);
  }

  onClose(): void {
    this.closePanel.emit();
  }
}
