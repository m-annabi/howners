import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { User } from '../../../core/models/user.model';
import { Subscription, filter } from 'rxjs';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
  roles?: string[];
}

interface NavSection {
  title: string;
  items: NavItem[];
  roles?: string[];
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit, OnDestroy {
  @Output() navigate = new EventEmitter<void>();

  currentUser: User | null = null;
  currentRoute = '';
  sections: NavSection[] = [];
  planName = 'FREE';
  private subs: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private subscriptionService: SubscriptionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.authService.currentUser$.subscribe(user => {
        this.currentUser = user;
        this.buildSections();
        if (user) {
          this.loadPlan();
        }
      })
    );

    this.subs.push(
      this.router.events.pipe(
        filter(e => e instanceof NavigationEnd)
      ).subscribe((e: any) => {
        this.currentRoute = e.urlAfterRedirects || e.url;
      })
    );

    this.currentRoute = this.router.url;
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  isActive(route: string): boolean {
    if (route === '/dashboard') {
      return this.currentRoute === '/dashboard';
    }
    return this.currentRoute.startsWith(route);
  }

  canViewSection(section: NavSection): boolean {
    if (!section.roles) return true;
    return !!this.currentUser && section.roles.includes(this.currentUser.role);
  }

  canViewItem(item: NavItem): boolean {
    if (!item.roles) return true;
    return !!this.currentUser && item.roles.includes(this.currentUser.role);
  }

  onItemClick(): void {
    this.navigate.emit();
  }

  get userPlan(): string {
    return this.planName;
  }

  private loadPlan(): void {
    this.subscriptionService.getUsageLimits().subscribe({
      next: (usage) => {
        this.planName = usage.planName || 'FREE';
      },
      error: () => {
        this.planName = 'FREE';
      }
    });
  }

  private buildSections(): void {
    this.sections = [
      {
        title: 'PRINCIPAL',
        items: [
          { label: 'Tableau de bord', icon: 'bi-grid-1x2', route: '/dashboard' },
        ]
      },
      {
        title: 'GESTION',
        items: [
          { label: 'Biens', icon: 'bi-building', route: '/properties', roles: ['OWNER', 'ADMIN'] },
          { label: 'Locations', icon: 'bi-key', route: '/rentals', roles: ['OWNER', 'ADMIN', 'CONCIERGE'] },
          { label: 'Contrats', icon: 'bi-file-earmark-text', route: '/contracts', roles: ['OWNER', 'ADMIN', 'CONCIERGE'] },
          { label: 'États des lieux', icon: 'bi-clipboard-check', route: '/inventory', roles: ['OWNER', 'ADMIN'] },
        ],
        roles: ['OWNER', 'ADMIN', 'CONCIERGE']
      },
      {
        title: 'ANNONCES',
        items: [
          { label: 'Rechercher', icon: 'bi-search', route: '/listings' },
          { label: 'Mes annonces', icon: 'bi-megaphone', route: '/listings/my', roles: ['OWNER', 'ADMIN'] },
          { label: 'Trouver locataires', icon: 'bi-person-lines-fill', route: '/tenant-discovery', roles: ['OWNER', 'ADMIN'] },
          { label: 'Candidatures', icon: 'bi-people', route: '/applications' },
          { label: 'Mon profil recherche', icon: 'bi-person-badge', route: '/search-profile', roles: ['TENANT'] },
          { label: 'Invitations', icon: 'bi-envelope-open', route: '/search-profile/invitations', roles: ['TENANT'] },
        ],
        roles: ['OWNER', 'ADMIN', 'CONCIERGE', 'TENANT']
      },
      {
        title: 'FINANCES',
        items: [
          { label: 'Paiements', icon: 'bi-credit-card', route: '/payments', roles: ['OWNER', 'ADMIN'] },
          { label: 'Factures', icon: 'bi-receipt', route: '/invoices', roles: ['OWNER', 'ADMIN', 'TENANT'] },
          { label: 'Quittances', icon: 'bi-file-earmark-check', route: '/receipts', roles: ['OWNER', 'ADMIN', 'TENANT'] },
          { label: 'Dépenses', icon: 'bi-wallet2', route: '/expenses', roles: ['OWNER', 'ADMIN'] },
          { label: 'Synthèse', icon: 'bi-graph-up', route: '/financial', roles: ['OWNER', 'ADMIN'] },
        ],
        roles: ['OWNER', 'ADMIN', 'TENANT']
      },
      {
        title: 'COMMUNICATION',
        items: [
          { label: 'Messages', icon: 'bi-chat-dots', route: '/messages' },
        ]
      },
      {
        title: 'ÉVALUATIONS',
        items: [
          { label: 'Notes', icon: 'bi-star', route: '/ratings' },
        ],
        roles: ['OWNER', 'CONCIERGE', 'ADMIN']
      }
    ];
  }
}
