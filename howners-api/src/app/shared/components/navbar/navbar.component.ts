import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
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
  private unreadInterval: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.authService.isAuthenticated$.subscribe(
      isAuth => {
        this.isAuthenticated = isAuth;
        if (isAuth) {
          this.loadUnreadCount();
          this.unreadInterval = setInterval(() => this.loadUnreadCount(), 30000);
        } else {
          this.unreadCount = 0;
          if (this.unreadInterval) {
            clearInterval(this.unreadInterval);
          }
        }
      }
    );

    this.authService.currentUser$.subscribe(
      user => {
        if (user) {
          this.userName = `${user.firstName} ${user.lastName}`;
          this.canViewRatings = ['OWNER', 'CONCIERGE', 'ADMIN'].includes(user.role);
          this.canViewFinances = ['OWNER', 'ADMIN'].includes(user.role);
          this.isAdmin = user.role === 'ADMIN';
        }
      }
    );
  }

  ngOnDestroy(): void {
    if (this.unreadInterval) {
      clearInterval(this.unreadInterval);
    }
  }

  private loadUnreadCount(): void {
    this.messageService.getUnreadCount().subscribe({
      next: (res) => this.unreadCount = res.count,
      error: () => {}
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
