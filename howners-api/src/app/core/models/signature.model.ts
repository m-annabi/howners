export interface Signature {
  id: string;
  contractId: string;
  signerId: string;
  signerFullName: string;
  signatureType: SignatureType;
  status: SignatureStatus;
  provider: string;
  ipAddress: string;
  signedAt: string;
  createdAt: string;
}

export enum SignatureType {
  SIMPLE = 'SIMPLE',
  ELECTRONIC = 'ELECTRONIC',
  QUALIFIED = 'QUALIFIED'
}

export enum SignatureStatus {
  PENDING = 'PENDING',
  SIGNED = 'SIGNED',
  REFUSED = 'REFUSED',
  EXPIRED = 'EXPIRED'
}

export interface CreateSignatureRequest {
  contractId: string;
  signatureData: string;  // Base64 image
  ipAddress?: string;
  userAgent?: string;
}

export const SIGNATURE_TYPE_LABELS: { [key in SignatureType]: string } = {
  [SignatureType.SIMPLE]: 'Simple',
  [SignatureType.ELECTRONIC]: 'Électronique',
  [SignatureType.QUALIFIED]: 'Qualifiée'
};

export const SIGNATURE_STATUS_LABELS: { [key in SignatureStatus]: string } = {
  [SignatureStatus.PENDING]: 'En attente',
  [SignatureStatus.SIGNED]: 'Signé',
  [SignatureStatus.REFUSED]: 'Refusé',
  [SignatureStatus.EXPIRED]: 'Expiré'
};
