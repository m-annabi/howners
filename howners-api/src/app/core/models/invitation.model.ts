export enum InvitationStatus {
  PENDING = 'PENDING',
  VIEWED = 'VIEWED',
  APPLIED = 'APPLIED',
  DECLINED = 'DECLINED'
}

export const INVITATION_STATUS_LABELS: Record<InvitationStatus, string> = {
  [InvitationStatus.PENDING]: 'En attente',
  [InvitationStatus.VIEWED]: 'Consultée',
  [InvitationStatus.APPLIED]: 'Candidature envoyée',
  [InvitationStatus.DECLINED]: 'Déclinée'
};

export const INVITATION_STATUS_COLORS: Record<InvitationStatus, string> = {
  [InvitationStatus.PENDING]: 'warning',
  [InvitationStatus.VIEWED]: 'info',
  [InvitationStatus.APPLIED]: 'success',
  [InvitationStatus.DECLINED]: 'danger'
};

export interface Invitation {
  id: string;
  listingId: string;
  listingTitle: string;
  tenantId: string;
  tenantName: string;
  ownerId: string;
  ownerName: string;
  message?: string;
  status: InvitationStatus;
  createdAt: string;
}

export interface CreateInvitationRequest {
  listingId: string;
  tenantId: string;
  message?: string;
}
