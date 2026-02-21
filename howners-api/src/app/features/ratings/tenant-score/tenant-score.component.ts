import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TenantScoringService } from '../../../core/services/tenant-scoring.service';
import {
  TenantScore,
  RISK_LEVEL_LABELS,
  RISK_LEVEL_COLORS
} from '../../../core/models/tenant-score.model';

@Component({
  selector: 'app-tenant-score',
  templateUrl: './tenant-score.component.html',
  styleUrls: ['./tenant-score.component.scss']
})
export class TenantScoreComponent implements OnInit {
  score: TenantScore | null = null;
  loading = true;
  tenantId = '';
  riskLabels = RISK_LEVEL_LABELS;
  riskColors = RISK_LEVEL_COLORS;

  // Arc length for 180° semicircle: π * r = π * 80 ≈ 251.3
  arcLength = 251.3;

  constructor(
    private scoringService: TenantScoringService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.paramMap.get('tenantId') || '';
    if (this.tenantId) {
      this.loadScore();
    }
  }

  loadScore(): void {
    this.loading = true;
    this.scoringService.getScore(this.tenantId).subscribe({
      next: (data) => {
        this.score = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getScoreColor(): string {
    if (!this.score) return 'var(--hw-gray-500)';
    if (this.score.score >= 80) return 'var(--hw-success)';
    if (this.score.score >= 60) return 'var(--hw-info)';
    if (this.score.score >= 40) return 'var(--hw-warning)';
    return 'var(--hw-danger)';
  }

  getArcOffset(): number {
    if (!this.score) return this.arcLength;
    return this.arcLength - (this.arcLength * this.score.score / 100);
  }
}
