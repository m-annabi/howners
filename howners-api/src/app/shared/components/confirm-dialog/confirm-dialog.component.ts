import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ConfirmDialogService, ConfirmDialogConfig } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent implements OnInit, OnDestroy {
  visible = false;
  config: ConfirmDialogConfig & { resolve: (result: boolean) => void } = {
    title: '',
    message: '',
    type: 'warning',
    resolve: () => {}
  };

  private sub?: Subscription;

  constructor(private confirmService: ConfirmDialogService) {}

  ngOnInit(): void {
    this.sub = this.confirmService.dialog$.subscribe(config => {
      this.config = config;
      this.visible = true;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onConfirm(): void {
    this.visible = false;
    this.config.resolve(true);
  }

  onCancel(): void {
    this.visible = false;
    this.config.resolve(false);
  }

  get iconClass(): string {
    return this.config.type === 'danger' ? 'bi-exclamation-triangle-fill' : 'bi-question-circle-fill';
  }

  get iconColorClass(): string {
    return this.config.type === 'danger' ? 'hw-icon-circle--danger' : 'hw-icon-circle--warning';
  }

  get confirmBtnClass(): string {
    return this.config.type === 'danger' ? 'hw-btn--danger' : 'hw-btn--primary';
  }
}
