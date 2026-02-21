import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FinancialDashboardService } from '../../../core/services/financial-dashboard.service';
import { FinancialDashboard } from '../../../core/models/financial-dashboard.model';

@Component({
  selector: 'app-financial-dashboard',
  templateUrl: './financial-dashboard.component.html',
  styleUrls: ['./financial-dashboard.component.scss']
})
export class FinancialDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  dashboard: FinancialDashboard | null = null;
  loading = false;
  error: string | null = null;
  maxMonthlyValue = 0;

  constructor(private financialService: FinancialDashboardService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.financialService.getDashboard().pipe(takeUntil(this.destroy$)).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.maxMonthlyValue = Math.max(
          ...dashboard.monthlyBreakdown.map(m => Math.max(m.revenue, m.expenses)),
          1
        );
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading financial dashboard:', err);
        this.error = 'Erreur lors du chargement du tableau de bord financier';
        this.loading = false;
      }
    });
  }

  getBarHeight(value: number): string {
    const percentage = (value / this.maxMonthlyValue) * 100;
    return Math.max(percentage, 2) + '%';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
