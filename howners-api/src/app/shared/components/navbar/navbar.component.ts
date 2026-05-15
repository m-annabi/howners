import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MessageService } from '../../../core/services/message.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  userName: string | null = null;
  canViewRatings = false;
  canViewFinances = false;
  isAdmin = false;
  unreadCount = 0;
  private subs: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.authService.isAuthenticated$.subscribe(isAuth => {
        this.isAuthenticated = isAuth;
        if (!isAuth) this.unreadCount = 0;
      })
    );

    this.subs.push(
      this.authService.currentUser$.subscribe(user => {
        if (user) {
          this.userName = `${user.firstName} ${user.lastName}`;
          this.canViewRatings = ['OWNER', 'CONCIERGE', 'ADMIN'].includes(user.role);
          this.canViewFinances = ['OWNER', 'ADMIN'].includes(user.role);
          this.isAdmin = user.role === 'ADMIN';
        }
      })
    );

    this.subs.push(
      this.messageService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
      })
    );

    // Remettre à zéro quand on navigue vers /messages
    this.subs.push(
      this.router.events.pipe(
        filter(e => e instanceof NavigationEnd)
      ).subscribe((e: any) => {
        const url: string = e.urlAfterRedirects || e.url;
        if (url.startsWith('/messages')) {
          this.messageService.resetUnreadCount();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
