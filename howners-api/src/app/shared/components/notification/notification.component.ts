import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService, Notification } from '../../../core/services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss']
})
export class NotificationComponent implements OnInit {
  notifications$!: Observable<Notification[]>;

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.notifications$ = this.notificationService.notifications$;
  }

  dismiss(id: string): void {
    this.notificationService.dismiss(id);
  }

  navigate(notification: Notification): void {
    this.notificationService.dismiss(notification.id);
    if (notification.route) {
      this.router.navigateByUrl(notification.route);
    }
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
