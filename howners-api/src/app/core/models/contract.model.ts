export interface Contract {
  id: string;
  contractNumber: string;
  rentalId: string;
  rentalPropertyName: string;
  tenantFullName: string;
  status: ContractStatus;
  currentVersion: number;
  documentUrl: string | null;
  createdAt: string;
  updatedAt: string | null;
  sentAt: string | null;
  signedAt: string | null;
}

export enum ContractStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  SIGNED = 'SIGNED',
  ACTIVE = 'ACTIVE',
  TERMINATED = 'TERMINATED',
  CANCELLED = 'CANCELLED'
}

export interface ContractVersion {
  id: string;
  contractId: string;
  version: number;
  content: string;
  documentUrl: string;
  documentHash: string;
  createdAt: string;
  createdById: string | null;
  createdByName: string;
}

export interface CreateContractRequest {
  rentalId: string;
  templateId?: string | null;
  customContent?: string;
}

export interface UpdateContractRequest {
  customContent?: string;
  status?: ContractStatus;
}

export const CONTRACT_STATUS_LABELS: { [key in ContractStatus]: string } = {
  [ContractStatus.DRAFT]: 'Brouillon',
  [ContractStatus.SENT]: 'Envoyé',
  [ContractStatus.SIGNED]: 'Signé',
  [ContractStatus.ACTIVE]: 'Actif',
  [ContractStatus.TERMINATED]: 'Terminé',
  [ContractStatus.CANCELLED]: 'Annulé'
};

export const CONTRACT_STATUS_COLORS: { [key in ContractStatus]: string } = {
  [ContractStatus.DRAFT]: '#6c757d',       // Gray
  [ContractStatus.SENT]: '#0dcaf0',        // Cyan
  [ContractStatus.SIGNED]: '#198754',      // Green
  [ContractStatus.ACTIVE]: '#0d6efd',      // Blue
  [ContractStatus.TERMINATED]: '#ffc107',  // Yellow
  [ContractStatus.CANCELLED]: '#dc3545'    // Red
};
