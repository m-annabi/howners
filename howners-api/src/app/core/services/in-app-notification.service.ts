import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { InAppNotification, InAppNotificationType } from '../models/in-app-notification.model';

const ICON_MAP: Record<InAppNotificationType, string> = {
  message:     'bi-chat-dots',
  payment:     'bi-credit-card',
  contract:    'bi-file-earmark-text',
  application: 'bi-people',
  inventory:   'bi-clipboard-check',
  system:      'bi-info-circle',
};

const MAX_NOTIFICATIONS = 50;

@Injectable({ providedIn: 'root' })
export class InAppNotificationService {
  private subject = new BehaviorSubject<InAppNotification[]>([]);

  readonly notifications$: Observable<InAppNotification[]> = this.subject.asObservable();

  readonly unreadCount$: Observable<number> = this.subject.pipe(
    map(list => list.filter(n => !n.isRead).length)
  );

  /**
   * Crée une notification in-app.
   * À appeler depuis n'importe quel service métier.
   *
   * @param type    Catégorie (détermine l'icône)
   * @param title   Titre court (ex: nom de l'expéditeur)
   * @param body    Corps du message (sera tronqué si trop long)
   * @param route   Route Angular de destination (optionnel)
   */
  add(type: InAppNotificationType, title: string, body: string, route?: string): void {
    const preview = body.length > 80 ? body.substring(0, 80) + '…' : body;
    const notification: InAppNotification = {
      id: this.generateId(),
      type,
      title,
      body: preview,
      route,
      icon: ICON_MAP[type],
      isRead: false,
      createdAt: new Date(),
    };
    const current = this.subject.value;
    this.subject.next([notification, ...current].slice(0, MAX_NOTIFICATIONS));
  }

  markAsRead(id: string): void {
    this.subject.next(
      this.subject.value.map(n => n.id === id ? { ...n, isRead: true } : n)
    );
  }

  markAllAsRead(): void {
    this.subject.next(
      this.subject.value.map(n => ({ ...n, isRead: true }))
    );
  }

  clear(): void {
    this.subject.next([]);
  }

  private generateId(): string {
    return `notif-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
  }
}
