export enum RentalStatus {
  VACANT     = 'VACANT',
  LISTED     = 'LISTED',
  PENDING    = 'PENDING',
  ACTIVE     = 'ACTIVE',
  EXITING    = 'EXITING',
  TERMINATED = 'TERMINATED',
  CANCELLED  = 'CANCELLED'
}

export interface Rental {
  id: string;
  propertyId: string;
  propertyName: string;
  tenantId?: string;
  tenantName?: string;
  tenantEmail?: string;
  status: RentalStatus;
  startDate?: string;
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
  startDate?: string;
  endDate?: string;
  monthlyRent: number;
  currency?: string;
  depositAmount?: number;
  charges?: number;
  paymentDay?: number;
}

export interface UpdateRentalRequest {
  status?: RentalStatus;
  startDate?: string;
  endDate?: string;
  monthlyRent?: number;
  currency?: string;
  depositAmount?: number;
  charges?: number;
  paymentDay?: number;
}

export interface PublishRentalRequest {
  title: string;
  description?: string;
  availableFrom?: string;
}

export interface ExitTenantRequest {
  exitDate: string;
  notes?: string;
}

export const RENTAL_STATUS_LABELS: Record<RentalStatus, string> = {
  [RentalStatus.VACANT]:     'Libre',
  [RentalStatus.LISTED]:     'En annonce',
  [RentalStatus.PENDING]:    'Contrat en attente',
  [RentalStatus.ACTIVE]:     'Active',
  [RentalStatus.EXITING]:    'Sortie programmée',
  [RentalStatus.TERMINATED]: 'Terminée',
  [RentalStatus.CANCELLED]:  'Annulée'
};

export const RENTAL_STATUS_COLORS: Record<RentalStatus, string> = {
  [RentalStatus.VACANT]:     '#9E9E9E',
  [RentalStatus.LISTED]:     '#2196F3',
  [RentalStatus.PENDING]:    '#FFA500',
  [RentalStatus.ACTIVE]:     '#4CAF50',
  [RentalStatus.EXITING]:    '#FF7043',
  [RentalStatus.TERMINATED]: '#607D8B',
  [RentalStatus.CANCELLED]:  '#F44336'
};
