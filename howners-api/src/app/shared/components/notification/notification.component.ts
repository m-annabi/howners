import { Component, OnInit } from '@angular/core';
import { NotificationService, Notification } from '../../../core/services/notification.service';
import { Observable } from 'rxjs';

/**
 * Component for displaying toast notifications
 * Positioned fixed at top-right of the screen
 */
@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss']
})
export class NotificationComponent implements OnInit {
  notifications$!: Observable<Notification[]>;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notifications$ = this.notificationService.notifications$;
  }

  /**
   * Dismiss a notification
   */
  dismiss(id: string): void {
    this.notificationService.dismiss(id);
  }

  /**
   * Get Bootstrap alert class based on notification type
   */
  getAlertClass(type: string): string {
    switch (type) {
      case 'success':
        return 'alert-success';
      case 'error':
        return 'alert-danger';
      case 'warning':
        return 'alert-warning';
      case 'info':
        return 'alert-info';
      default:
        return 'alert-info';
    }
  }
}
