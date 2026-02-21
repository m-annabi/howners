import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TenantScoringService } from '../../../core/services/tenant-scoring.service';
import {
  TenantScore,
  RISK_LEVEL_LABELS,
  RISK_LEVEL_COLORS
} from '../../../core/models/tenant-score.model';

@Component({
  selector: 'app-tenant-comparison',
  templateUrl: './tenant-comparison.component.html',
  styleUrls: ['./tenant-comparison.component.scss']
})
export class TenantComparisonComponent implements OnInit {
  scores: TenantScore[] = [];
  loading = true;
  riskLabels = RISK_LEVEL_LABELS;
  riskColors = RISK_LEVEL_COLORS;

  constructor(
    private scoringService: TenantScoringService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const tenantIdsParam = this.route.snapshot.queryParamMap.get('tenantIds');
    if (tenantIdsParam) {
      const tenantIds = tenantIdsParam.split(',');
      this.loadComparison(tenantIds);
    } else {
      this.loading = false;
    }
  }

  loadComparison(tenantIds: string[]): void {
    this.loading = true;
    this.scoringService.compareScores(tenantIds).subscribe({
      next: (data) => {
        this.scores = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getScoreColor(score: number): string {
    if (score >= 80) return 'var(--hw-success)';
    if (score >= 60) return 'var(--hw-info)';
    if (score >= 40) return 'var(--hw-warning)';
    return 'var(--hw-danger)';
  }

  getBestScore(): TenantScore | null {
    if (this.scores.length === 0) return null;
    return this.scores.reduce((best, s) => s.score > best.score ? s : best, this.scores[0]);
  }
}
