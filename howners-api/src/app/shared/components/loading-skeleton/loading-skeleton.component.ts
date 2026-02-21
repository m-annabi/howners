import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-skeleton',
  templateUrl: './loading-skeleton.component.html',
  styleUrls: ['./loading-skeleton.component.scss']
})
export class LoadingSkeletonComponent {
  @Input() type: 'card' | 'table' | 'detail' | 'list' = 'card';
  @Input() count = 3;

  get items(): number[] {
    return Array(this.count).fill(0);
  }
}
