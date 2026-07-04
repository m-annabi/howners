export interface OwnerRating {
  id: string;
  ownerId: string;
  raterName: string;
  rentalId: string | null;
  propertyName: string | null;
  communicationRating: number;
  responsivenessRating: number;
  contractRespectRating: number;
  overallRating: number;
  comment: string | null;
  createdAt: string;
}

export interface CreateOwnerRatingRequest {
  ownerId: string;
  rentalId?: string;
  communicationRating: number;
  responsivenessRating: number;
  contractRespectRating: number;
  comment?: string;
}

export const OWNER_RATING_LABELS: Record<string, string> = {
  communicationRating: 'Communication',
  responsivenessRating: 'Réactivité',
  contractRespectRating: 'Respect du contrat',
};
