export interface TenantRating {
  id: string;
  tenantId: string;
  tenantName: string;
  raterId: string;
  raterName: string;
  rentalId?: string;
  propertyName?: string;
  paymentRating: number;
  propertyRespectRating: number;
  communicationRating: number;
  overallRating: number;
  comment?: string;
  ratingPeriod?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantRatingSummary {
  averagePaymentRating: number | null;
  averagePropertyRespectRating: number | null;
  averageCommunicationRating: number | null;
  averageOverallRating: number | null;
  totalRatings: number;
}

export interface CreateTenantRatingRequest {
  tenantId: string;
  rentalId?: string;
  paymentRating: number;
  propertyRespectRating: number;
  communicationRating: number;
  comment?: string;
  ratingPeriod?: string;
}

export interface UpdateTenantRatingRequest {
  paymentRating?: number;
  propertyRespectRating?: number;
  communicationRating?: number;
  comment?: string;
  ratingPeriod?: string;
}

export const RATING_CRITERIA_LABELS: Record<string, string> = {
  paymentRating: 'Paiement à temps',
  propertyRespectRating: 'Respect du logement',
  communicationRating: 'Communication'
};

export const RATING_CRITERIA_ICONS: Record<string, string> = {
  paymentRating: 'bi-cash-coin',
  propertyRespectRating: 'bi-house-check',
  communicationRating: 'bi-chat-dots'
};
