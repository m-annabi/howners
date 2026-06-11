import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface AdminStats {
  totalUsers: number;
  totalOwners: number;
  totalTenants: number;
  totalProperties: number;
  totalRentals: number;
  totalContracts: number;
  newUsersThisMonth: number;
  activeSubscriptionsByPlan: { [plan: string]: number };
  mrr: number;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  stats: AdminStats | null = null;
  loading = true;
  error: string | null = null;

  get planEntries(): { plan: string; count: number }[] {
    if (!this.stats?.activeSubscriptionsByPlan) return [];
    return Object.entries(this.stats.activeSubscriptionsByPlan)
      .map(([plan, count]) => ({ plan, count }));
  }

  get totalSubscriptions(): number {
    return this.planEntries.reduce((sum, e) => sum + e.count, 0);
  }

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.error = null;
    this.http.get<AdminStats>(`${environment.apiUrl}/admin/stats`).subscribe({
      next: stats => {
        this.stats = stats;
        this.loading = false;
      },
      error: err => {
        this.error = err.error?.message || 'Erreur lors du chargement des statistiques admin';
        this.loading = false;
      }
    });
  }

  getPlanBarWidth(count: number): number {
    if (this.totalSubscriptions <= 0) return 0;
    return Math.round((count / this.totalSubscriptions) * 100);
  }

  getPlanColor(plan: string): string {
    switch (plan) {
      case 'FREE': return 'var(--hw-gray-400)';
      case 'PRO': return 'var(--hw-primary-600)';
      case 'PREMIUM': return 'var(--hw-accent-500)';
      default: return 'var(--hw-gray-300)';
    }
  }
}
