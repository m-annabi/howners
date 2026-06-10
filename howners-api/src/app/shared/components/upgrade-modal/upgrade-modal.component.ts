import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Subscription } from 'rxjs';
import { UpgradeModalService } from '../../../core/services/upgrade-modal.service';

@Component({
  selector: 'app-upgrade-modal',
  templateUrl: './upgrade-modal.component.html',
  styleUrls: ['./upgrade-modal.component.scss']
})
export class UpgradeModalComponent implements OnInit, OnDestroy {
  visible = false;
  message = '';

  private sub?: Subscription;

  constructor(private upgradeModalService: UpgradeModalService) {}

  ngOnInit(): void {
    this.sub = this.upgradeModalService.state$.subscribe(state => {
      this.visible = state.visible;
      this.message = state.message;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onClose(): void {
    this.upgradeModalService.hide();
  }

  onViewPlans(): void {
    this.upgradeModalService.hide();
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.visible) {
      this.onClose();
    }
  }
}
