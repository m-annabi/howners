import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { MessageService } from '../../../core/services/message.service';
import { User } from '../../../core/models/user.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Output() toggleSidebar = new EventEmitter<void>();

  currentUser: User | null = null;
  unreadCount = 0;
  dropdownOpen = false;
  private subs: Subscription[] = [];
  private unreadInterval: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.authService.currentUser$.subscribe(user => {
        this.currentUser = user;
      })
    );

    this.loadUnreadCount();
    this.unreadInterval = setInterval(() => this.loadUnreadCount(), 30000);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    if (this.unreadInterval) clearInterval(this.unreadInterval);
  }

  get userName(): string {
    if (!this.currentUser) return 'Utilisateur';
    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }

  get userInitials(): string {
    if (!this.currentUser) return 'U';
    return `${this.currentUser.firstName?.charAt(0) || ''}${this.currentUser.lastName?.charAt(0) || ''}`.toUpperCase();
  }

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    this.closeDropdown();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  private loadUnreadCount(): void {
    this.messageService.getUnreadCount().subscribe({
      next: (res) => this.unreadCount = res.count,
      error: () => {}
    });
  }
}
