export interface SubscriptionPlan {
  id: string;
  name: PlanName;
  displayName: string;
  monthlyPrice: number;
  annualPrice: number;
  maxProperties: number;
  maxContractsPerMonth: number;
  features: { [key: string]: boolean };
}

export enum PlanName {
  FREE = 'FREE',
  PRO = 'PRO',
  PREMIUM = 'PREMIUM'
}

export interface UserSubscription {
  id: string;
  userId: string;
  plan: SubscriptionPlan;
  status: SubscriptionStatus;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  createdAt: string;
}

export enum SubscriptionStatus {
  ACTIVE = 'ACTIVE',
  PAST_DUE = 'PAST_DUE',
  CANCELLED = 'CANCELLED',
  TRIALING = 'TRIALING'
}

export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

export interface UsageLimits {
  planName: string;
  currentProperties: number;
  maxProperties: number;
  currentContractsThisMonth: number;
  maxContractsPerMonth: number;
  canCreateProperty: boolean;
  canCreateContract: boolean;
}

export const PLAN_FEATURES: { [key in PlanName]: string[] } = {
  [PlanName.FREE]: [
    'Jusqu\'a 2 propriétés',
    '3 contrats par mois',
    'Documents',
    'Messagerie'
  ],
  [PlanName.PRO]: [
    'Jusqu\'a 10 propriétés',
    'Contrats illimités',
    'Signature électronique',
    'Scoring locataire',
    'Rapports avancés'
  ],
  [PlanName.PREMIUM]: [
    'Propriétés illimitées',
    'Contrats illimités',
    'Toutes les fonctionnalités Pro',
    'Chiffrement des documents',
    'Support prioritaire',
    'Accès API'
  ]
};

export const PLAN_COLORS: { [key in PlanName]: string } = {
  [PlanName.FREE]: 'secondary',
  [PlanName.PRO]: 'primary',
  [PlanName.PREMIUM]: 'warning'
};
