export interface ContractAmendment {
  id: string;
  contractId: string;
  contractNumber: string;
  amendmentNumber: number;
  reason: string;
  changes: string | null;
  previousRent: number | null;
  newRent: number | null;
  effectiveDate: string;
  status: AmendmentStatus;
  createdByName: string;
  signedAt: string | null;
  documentId: string | null;
  createdAt: string;
}

export enum AmendmentStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  SIGNED = 'SIGNED',
  CANCELLED = 'CANCELLED'
}

export interface CreateAmendmentRequest {
  reason: string;
  changes?: string;
  previousRent?: number;
  newRent?: number;
  effectiveDate: string;
}

export const AMENDMENT_STATUS_LABELS: { [key in AmendmentStatus]: string } = {
  [AmendmentStatus.DRAFT]: 'Brouillon',
  [AmendmentStatus.SENT]: 'Envoyé',
  [AmendmentStatus.SIGNED]: 'Signé',
  [AmendmentStatus.CANCELLED]: 'Annulé'
};

export const AMENDMENT_STATUS_COLORS: { [key in AmendmentStatus]: string } = {
  [AmendmentStatus.DRAFT]: 'secondary',
  [AmendmentStatus.SENT]: 'info',
  [AmendmentStatus.SIGNED]: 'success',
  [AmendmentStatus.CANCELLED]: 'danger'
};
