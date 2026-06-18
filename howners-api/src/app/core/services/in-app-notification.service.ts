import { Injectable, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable, Subscription, interval, of } from 'rxjs';
import { map, switchMap, catchError, filter } from 'rxjs/operators';
import { InAppNotification, InAppNotificationType } from '../models/in-app-notification.model';
import { NotificationCenterService, ServerNotification } from './notification-center.service';
import { StorageService } from './storage.service';

const ICON_MAP: Record<string, string> = {
  PAYMENT_REMINDER:     'bi-credit-card',
  PAYMENT_OVERDUE:      'bi-exclamation-triangle',
  CONTRACT_EXPIRY:      'bi-file-earmark-text',
  APPLICATION_RECEIVED: 'bi-people',
  APPLICATION_ACCEPTED: 'bi-check-circle',
  APPLICATION_REJECTED: 'bi-x-circle',
  SIGNATURE_COMPLETED:  'bi-pen',
  SYSTEM:               'bi-info-circle',
  message:     'bi-chat-dots',
  payment:     'bi-credit-card',
  contract:    'bi-file-earmark-text',
  application: 'bi-people',
  inventory:   'bi-clipboard-check',
  system:      'bi-info-circle',
};

@Injectable({ providedIn: 'root' })
export class InAppNotificationService implements OnDestroy {
  private subject = new BehaviorSubject<InAppNotification[]>([]);
  private countSubject = new BehaviorSubject<number>(0);
  private pollSub?: Subscription;

  readonly notifications$: Observable<InAppNotification[]> = this.subject.asObservable();

  readonly unreadCount$: Observable<number> = this.countSubject.asObservable();

  constructor(
    private notificationCenterService: NotificationCenterService,
    private storageService: StorageService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    // En SSR/prerender : pas de polling (un interval garderait l'app instable → render bloqué).
    if (!isPlatformBrowser(platformId)) {
      return;
    }
    if (this.isAuthenticated()) {
      this.loadNotifications();
      this.loadUnreadCount();
    }
    this.pollSub = interval(60_000).subscribe(() => {
      if (this.isAuthenticated()) {
        this.loadUnreadCount();
      }
    });
  }

  private isAuthenticated(): boolean {
    return !!this.storageService.getItem('access_token');
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  /**
   * Charge les notifications récentes depuis le serveur.
   */
  loadNotifications(): void {
    this.notificationCenterService.getNotifications(0, 30).pipe(
      map(page => page.content.map(n => this.toInAppNotification(n))),
      catchError(() => of([] as InAppNotification[]))
    ).subscribe(notifications => {
      this.subject.next(notifications);
      this.countSubject.next(notifications.filter(n => !n.isRead).length);
    });
  }

  /**
   * Charge uniquement le compteur de notifications non lues.
   */
  loadUnreadCount(): void {
    this.notificationCenterService.getUnreadCount().pipe(
      catchError(() => of({ count: 0 }))
    ).subscribe(result => {
      this.countSubject.next(result.count);
    });
  }

  markAsRead(id: string): void {
    this.notificationCenterService.markAsRead(id).pipe(
      catchError(() => of(void 0))
    ).subscribe(() => {
      // Mise à jour optimiste locale
      this.subject.next(
        this.subject.value.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
      this.countSubject.next(
        this.subject.value.filter(n => !n.isRead).length
      );
    });
  }

  markAllAsRead(): void {
    this.notificationCenterService.markAllAsRead().pipe(
      catchError(() => of(void 0))
    ).subscribe(() => {
      this.subject.next(
        this.subject.value.map(n => ({ ...n, isRead: true }))
      );
      this.countSubject.next(0);
    });
  }

  /**
   * Permet toujours d'ajouter des notifications purement client-side (rétrocompatibilité).
   */
  add(type: InAppNotificationType, title: string, body: string, route?: string): void {
    const preview = body.length > 80 ? body.substring(0, 80) + '…' : body;
    const notification: InAppNotification = {
      id: `local-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      type,
      title,
      body: preview,
      route,
      icon: ICON_MAP[type] || 'bi-info-circle',
      isRead: false,
      createdAt: new Date(),
    };
    const current = this.subject.value;
    this.subject.next([notification, ...current].slice(0, 50));
    this.countSubject.next(this.subject.value.filter(n => !n.isRead).length);
  }

  clear(): void {
    this.subject.next([]);
    this.countSubject.next(0);
  }

  private toInAppNotification(server: ServerNotification): InAppNotification {
    return {
      id: server.id,
      type: (server.type?.toLowerCase() || 'system') as InAppNotificationType,
      title: server.title,
      body: server.message || '',
      route: server.link || undefined,
      icon: ICON_MAP[server.type] || 'bi-info-circle',
      isRead: server.read,
      createdAt: new Date(server.createdAt),
    };
  }
}
