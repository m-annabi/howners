export interface TenantRating {
  id: string;
  tenantId: string;
  raterName: string;
  rentalId: string | null;
  propertyName: string | null;
  paymentRating: number;
  propertyRespectRating: number;
  communicationRating: number;
  overallRating: number;
  comment: string | null;
  createdAt: string;
}

export interface CreateTenantRatingRequest {
  tenantId: string;
  rentalId?: string;
  paymentRating: number;
  propertyRespectRating: number;
  communicationRating: number;
  comment?: string;
}

export const RATING_LABELS: Record<string, string> = {
  paymentRating: 'Paiement du loyer',
  propertyRespectRating: 'Respect du bien',
  communicationRating: 'Communication',
};
