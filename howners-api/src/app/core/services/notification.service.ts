import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export enum NotificationType {
  SUCCESS = 'success',
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info'
}

export interface Notification {
  type: NotificationType;
  message: string;
  duration?: number;
  id: string;
}

/**
 * Service for displaying toast notifications to the user
 * Replaces browser alert() calls with a better UX
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$: Observable<Notification[]> = this.notificationsSubject.asObservable();

  /**
   * Show a notification
   */
  show(type: NotificationType, message: string, duration: number = 5000): void {
    const notification: Notification = {
      type,
      message,
      duration,
      id: this.generateId()
    };

    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([...current, notification]);

    if (duration && duration > 0) {
      setTimeout(() => this.dismiss(notification.id), duration);
    }
  }

  /**
   * Show a success notification
   */
  success(message: string, duration: number = 3000): void {
    this.show(NotificationType.SUCCESS, message, duration);
  }

  /**
   * Show an error notification
   */
  error(message: string, duration: number = 5000): void {
    this.show(NotificationType.ERROR, message, duration);
  }

  /**
   * Show a warning notification
   */
  warning(message: string, duration: number = 4000): void {
    this.show(NotificationType.WARNING, message, duration);
  }

  /**
   * Show an info notification
   */
  info(message: string, duration: number = 3000): void {
    this.show(NotificationType.INFO, message, duration);
  }

  /**
   * Dismiss a notification by ID
   */
  dismiss(id: string): void {
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next(current.filter(n => n.id !== id));
  }

  /**
   * Dismiss all notifications
   */
  dismissAll(): void {
    this.notificationsSubject.next([]);
  }

  /**
   * Generate a unique ID for a notification
   */
  private generateId(): string {
    return `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}
