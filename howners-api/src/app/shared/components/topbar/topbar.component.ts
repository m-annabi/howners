import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription, filter, Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MessageService } from '../../../core/services/message.service';
import { InAppNotificationService } from '../../../core/services/in-app-notification.service';
import { InAppNotification } from '../../../core/models/in-app-notification.model';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Output() toggleSidebar = new EventEmitter<void>();

  currentUser: User | null = null;
  dropdownOpen = false;
  notifPanelOpen = false;

  notifications$!: Observable<InAppNotification[]>;
  unreadCount$!: Observable<number>;

  private subs: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService,
    private inAppNotificationService: InAppNotificationService
  ) {}

  ngOnInit(): void {
    this.notifications$ = this.inAppNotificationService.notifications$;
    this.unreadCount$ = this.inAppNotificationService.unreadCount$;

    this.subs.push(
      this.authService.currentUser$.subscribe(user => {
        this.currentUser = user;
      })
    );

    this.subs.push(
      this.router.events.pipe(
        filter(e => e instanceof NavigationEnd)
      ).subscribe((e: any) => {
        const url: string = e.urlAfterRedirects || e.url;
        if (url.startsWith('/messages')) {
          this.messageService.resetUnreadCount();
        }
        this.notifPanelOpen = false;
        this.dropdownOpen = false;
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  get userName(): string {
    if (!this.currentUser) return 'Utilisateur';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }

  get userFirstName(): string {
    return this.currentUser?.firstName || 'Compte';
  }

  get userInitials(): string {
    if (!this.currentUser) return 'U';
    return `${this.currentUser.firstName?.charAt(0) || ''}${this.currentUser.lastName?.charAt(0) || ''}`.toUpperCase();
  }

  get userRoleLabel(): string {
    const labels: Record<string, string> = {
      OWNER: 'Propriétaire',
      TENANT: 'Locataire',
      ADMIN: 'Administrateur',
      CONCIERGE: 'Gestionnaire',
    };
    return this.currentUser?.role ? (labels[this.currentUser.role] || this.currentUser.role) : '';
  }

  timeAgo(date: Date): string {
    const diff = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
    if (diff < 60) return 'À l\'instant';
    if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)}h`;
    if (diff < 172800) return 'Hier';
    return new Date(date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  }

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
    if (this.dropdownOpen) this.notifPanelOpen = false;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  toggleNotifPanel(): void {
    this.notifPanelOpen = !this.notifPanelOpen;
    if (this.notifPanelOpen) {
      this.dropdownOpen = false;
      this.inAppNotificationService.loadNotifications();
    }
  }

  closeNotifPanel(): void {
    this.notifPanelOpen = false;
  }

  openNotification(notif: InAppNotification): void {
    this.inAppNotificationService.markAsRead(notif.id);
    this.notifPanelOpen = false;
    if (notif.route) {
      this.router.navigateByUrl(notif.route);
    }
  }

  markAllAsRead(): void {
    this.inAppNotificationService.markAllAsRead();
  }

  logout(): void {
    this.closeDropdown();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
