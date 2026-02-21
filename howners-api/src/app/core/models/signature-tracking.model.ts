export interface SignatureRequest {
  id: string;
  contractId: string;
  contractNumber: string;
  provider: string;
  providerEnvelopeId: string;
  signerEmail: string;
  signerName: string;
  status: SignatureRequestStatus;
  signingUrl: string | null;
  sentAt: string | null;
  viewedAt: string | null;
  signedAt: string | null;
  declinedAt: string | null;
  tokenExpiresAt: string;
  resendCount: number;
  declineReason: string | null;
  reminderCount: number;
  lastReminderAt: string | null;
  signerOrder: number;
}

export enum SignatureRequestStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  VIEWED = 'VIEWED',
  SIGNED = 'SIGNED',
  DECLINED = 'DECLINED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED'
}

export interface SignatureTrackingDashboard {
  totalRequests: number;
  pendingRequests: number;
  sentRequests: number;
  viewedRequests: number;
  signedRequests: number;
  declinedRequests: number;
  expiredRequests: number;
  recentRequests: SignatureRequest[];
}

export interface SignerInfo {
  email: string;
  name: string;
  order: number;
}

export const SIGNATURE_STATUS_LABELS: { [key in SignatureRequestStatus]: string } = {
  [SignatureRequestStatus.PENDING]: 'En attente',
  [SignatureRequestStatus.SENT]: 'Envoyé',
  [SignatureRequestStatus.VIEWED]: 'Consulté',
  [SignatureRequestStatus.SIGNED]: 'Signé',
  [SignatureRequestStatus.DECLINED]: 'Refusé',
  [SignatureRequestStatus.CANCELLED]: 'Annulé',
  [SignatureRequestStatus.EXPIRED]: 'Expiré'
};

export const SIGNATURE_STATUS_COLORS: { [key in SignatureRequestStatus]: string } = {
  [SignatureRequestStatus.PENDING]: 'secondary',
  [SignatureRequestStatus.SENT]: 'info',
  [SignatureRequestStatus.VIEWED]: 'primary',
  [SignatureRequestStatus.SIGNED]: 'success',
  [SignatureRequestStatus.DECLINED]: 'danger',
  [SignatureRequestStatus.CANCELLED]: 'dark',
  [SignatureRequestStatus.EXPIRED]: 'warning'
};
