export interface SubscriptionPlan {
  id: string;
  name: PlanName;
  displayName: string;
  monthlyPrice: number;
  annualPrice: number;
  maxProperties: number;
  maxContractsPerMonth: number;
  platformFeePercent: number;
  features: { [key: string]: boolean };
}

export enum PlanName {
  FREE = 'FREE',
  PRO = 'PRO',
  PREMIUM = 'PREMIUM',
  AGENCE = 'AGENCE'
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
  canCreateRental: boolean;
  canCreateListing: boolean;
  hasESignature: boolean;
  hasDocumentEncryption: boolean;
  hasMultiAccount: boolean;
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
    'Révision de loyer IRL',
    'Export fiscal 2044',
    'Dashboard patrimonial',
    'Rapports avancés'
  ],
  [PlanName.PREMIUM]: [
    'Propriétés illimitées',
    'Contrats illimités',
    'Toutes les fonctionnalités Pro',
    'Chiffrement des documents',
    'Support prioritaire',
    'Accès API'
  ],
  [PlanName.AGENCE]: [
    'Tout illimité',
    'Toutes les fonctionnalités Premium',
    'Comptes délégués (multi-comptes)',
    'Commission réduite sur les loyers',
    'Pensé pour agences et SCI'
  ]
};

export const PLAN_COLORS: { [key in PlanName]: string } = {
  [PlanName.FREE]: 'secondary',
  [PlanName.PRO]: 'primary',
  [PlanName.PREMIUM]: 'warning',
  [PlanName.AGENCE]: 'dark'
};
