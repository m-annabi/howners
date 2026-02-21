import { Component, Input, TemplateRef } from '@angular/core';

export interface Breadcrumb {
  label: string;
  route?: string;
}

@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styleUrls: ['./page-header.component.scss']
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle?: string;
  @Input() breadcrumbs: Breadcrumb[] = [];
  @Input() actions?: TemplateRef<any>;
}
