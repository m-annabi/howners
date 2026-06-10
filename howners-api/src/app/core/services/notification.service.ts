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
  route?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$: Observable<Notification[]> = this.notificationsSubject.asObservable();

  show(type: NotificationType, message: string, duration: number = 5000, route?: string): void {
    const notification: Notification = {
      type,
      message,
      duration,
      id: this.generateId(),
      route
    };

    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([...current, notification]);

    if (duration && duration > 0) {
      setTimeout(() => this.dismiss(notification.id), duration);
    }
  }

  success(message: string, duration: number = 3000): void {
    this.show(NotificationType.SUCCESS, message, duration);
  }

  error(message: string, duration: number = 5000): void {
    this.show(NotificationType.ERROR, message, duration);
  }

  warning(message: string, duration: number = 4000): void {
    this.show(NotificationType.WARNING, message, duration);
  }

  info(message: string, duration: number = 3000, route?: string): void {
    this.show(NotificationType.INFO, message, duration, route);
  }

  dismiss(id: string): void {
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next(current.filter(n => n.id !== id));
  }

  dismissAll(): void {
    this.notificationsSubject.next([]);
  }

  private generateId(): string {
    return `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}
