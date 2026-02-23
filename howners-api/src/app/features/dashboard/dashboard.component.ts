import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { User } from '../../core/models/user.model';
import { DashboardStats } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  stats: DashboardStats | null = null;
  loading = true;
  error: string | null = null;

  get isTenant(): boolean {
    return this.currentUser?.role === 'TENANT';
  }

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.loadStats();
  }

  get hasTenantAlerts(): boolean {
    return !!this.stats?.tenantInfo && (
      this.stats.tenantInfo.pendingInvitations > 0 ||
      this.stats.tenantInfo.pendingApplications > 0 ||
      !this.stats.tenantInfo.searchProfileActive
    );
  }

  get hasActivity(): boolean {
    return !!this.stats && (
      this.stats.pendingRentals > 0 ||
      !!this.stats.recentActivity.latestProperty ||
      !!this.stats.recentActivity.latestRental
    );
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;

    this.dashboardService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement des statistiques';
        this.loading = false;
      }
    });
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}
