export interface ListingPhoto {
  id: string;
  listingId: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  mimeType: string;
  caption?: string;
  displayOrder: number;
  isPrimary: boolean;
  uploaderId?: string;
  uploaderName?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Listing {
  id: string;
  propertyId: string;
  propertyName: string;
  propertyCity: string;
  propertyPostalCode: string | null;
  propertyDepartment: string | null;
  propertyCountry: string | null;
  ownerId: string;
  ownerName: string;
  title: string;
  description: string;
  pricePerNight: number | null;
  pricePerMonth: number | null;
  currency: string;
  minStay: number | null;
  maxStay: number | null;
  amenities: string[] | null;
  requirements: string[] | null;
  availableFrom: string | null;
  status: ListingStatus;
  photos: ListingPhoto[];
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export enum ListingStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  PAUSED = 'PAUSED',
  CLOSED = 'CLOSED'
}

export interface CreateListingRequest {
  propertyId: string;
  title: string;
  description?: string;
  pricePerNight?: number;
  pricePerMonth?: number;
  currency?: string;
  minStay?: number;
  maxStay?: number;
  amenities?: string[];
  requirements?: string[];
  availableFrom?: string;
}

export const LISTING_STATUS_LABELS: { [key in ListingStatus]: string } = {
  [ListingStatus.DRAFT]: 'Brouillon',
  [ListingStatus.PUBLISHED]: 'Publiée',
  [ListingStatus.PAUSED]: 'En pause',
  [ListingStatus.CLOSED]: 'Fermée'
};

export const LISTING_STATUS_COLORS: { [key in ListingStatus]: string } = {
  [ListingStatus.DRAFT]: 'secondary',
  [ListingStatus.PUBLISHED]: 'success',
  [ListingStatus.PAUSED]: 'warning',
  [ListingStatus.CLOSED]: 'danger'
};
