import { Document } from './document.model';

export interface Application {
  id: string;
  listingId: string;
  listingTitle: string;
  propertyName: string;
  applicantId: string;
  applicantName: string;
  applicantEmail: string;
  coverLetter: string | null;
  desiredMoveIn: string | null;
  status: ApplicationStatus;
  reviewedAt: string | null;
  reviewedByName: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string | null;
  documents: Document[];
}

export enum ApplicationStatus {
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN'
}

export interface CreateApplicationRequest {
  listingId: string;
  coverLetter?: string;
  desiredMoveIn?: string;
}

export interface ReviewApplicationRequest {
  status: ApplicationStatus;
  notes?: string;
}

export const APPLICATION_STATUS_LABELS: { [key in ApplicationStatus]: string } = {
  [ApplicationStatus.SUBMITTED]: 'Soumise',
  [ApplicationStatus.UNDER_REVIEW]: 'En cours d\'examen',
  [ApplicationStatus.ACCEPTED]: 'Acceptée',
  [ApplicationStatus.REJECTED]: 'Refusée',
  [ApplicationStatus.WITHDRAWN]: 'Retirée'
};

export const APPLICATION_STATUS_COLORS: { [key in ApplicationStatus]: string } = {
  [ApplicationStatus.SUBMITTED]: 'primary',
  [ApplicationStatus.UNDER_REVIEW]: 'info',
  [ApplicationStatus.ACCEPTED]: 'success',
  [ApplicationStatus.REJECTED]: 'danger',
  [ApplicationStatus.WITHDRAWN]: 'secondary'
};
