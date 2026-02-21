export interface TenantScore {
  tenantId: string;
  tenantName: string;
  score: number;
  riskLevel: RiskLevel;
  breakdown: ScoreBreakdown;
  paymentStats: PaymentStats;
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export interface ScoreBreakdown {
  paymentScore: number;
  ratingScore: number;
  leaseDurationScore: number;
  communicationScore: number;
}

export interface PaymentStats {
  totalPayments: number;
  onTimePayments: number;
  latePayments: number;
  onTimePercentage: number;
}

export const RISK_LEVEL_LABELS: { [key in RiskLevel]: string } = {
  [RiskLevel.LOW]: 'Faible',
  [RiskLevel.MEDIUM]: 'Moyen',
  [RiskLevel.HIGH]: 'Élevé'
};

export const RISK_LEVEL_COLORS: { [key in RiskLevel]: string } = {
  [RiskLevel.LOW]: 'success',
  [RiskLevel.MEDIUM]: 'warning',
  [RiskLevel.HIGH]: 'danger'
};
