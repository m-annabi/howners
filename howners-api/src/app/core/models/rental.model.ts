export enum RentalType {
  SHORT_TERM = 'SHORT_TERM',
  LONG_TERM = 'LONG_TERM'
}

export enum RentalStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  TERMINATED = 'TERMINATED',
  CANCELLED = 'CANCELLED'
}

export interface Rental {
  id: string;
  propertyId: string;
  propertyName: string;
  tenantId?: string;
  tenantName?: string;
  tenantEmail?: string;
  rentalType: RentalType;
  status: RentalStatus;
  startDate: string;
  endDate?: string;
  monthlyRent: number;
  currency: string;
  depositAmount?: number;
  charges?: number;
  paymentDay?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRentalRequest {
  propertyId: string;
  tenantId?: string;
  tenantEmail?: string;
  tenantFirstName?: string;
  tenantLastName?: string;
  tenantPhone?: string;
  rentalType: RentalType;
  startDate: string;
  endDate?: string;
  monthlyRent: number;
  currency?: string;
  depositAmount?: number;
  charges?: number;
  paymentDay?: number;
}

export interface UpdateRentalRequest {
  rentalType?: RentalType;
  status?: RentalStatus;
  startDate?: string;
  endDate?: string;
  monthlyRent?: number;
  currency?: string;
  depositAmount?: number;
  charges?: number;
  paymentDay?: number;
}

export const RENTAL_TYPE_LABELS: Record<RentalType, string> = {
  [RentalType.SHORT_TERM]: 'Courte durée',
  [RentalType.LONG_TERM]: 'Longue durée'
};

export const RENTAL_STATUS_LABELS: Record<RentalStatus, string> = {
  [RentalStatus.PENDING]: 'En attente',
  [RentalStatus.ACTIVE]: 'Active',
  [RentalStatus.TERMINATED]: 'Terminée',
  [RentalStatus.CANCELLED]: 'Annulée'
};

export const RENTAL_STATUS_COLORS: Record<RentalStatus, string> = {
  [RentalStatus.PENDING]: '#FFA500',
  [RentalStatus.ACTIVE]: '#4CAF50',
  [RentalStatus.TERMINATED]: '#9E9E9E',
  [RentalStatus.CANCELLED]: '#F44336'
};
