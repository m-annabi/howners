/**
 * Models pour la signature électronique de contrats
 */
import { SignatureRequestStatus } from './signature-tracking.model';

// Ré-export pour les consommateurs existants
export { SignatureRequestStatus };

/**
 * Réponse contenant les informations d'une demande de signature
 */
export interface SignatureRequestResponse {
  id: string;
  contractId: string;
  contractNumber: string;
  provider: string;
  signerEmail: string;
  signerName: string;
  status: SignatureRequestStatus;
  sentAt?: string;
  viewedAt?: string;
  signedAt?: string;
  declinedAt?: string;
  tokenExpiresAt: string;
  resendCount: number;
  declineReason?: string;
  reminderCount?: number;
  lastReminderAt?: string;
  signerOrder?: number;
}

/**
 * Vue publique d'un contrat (accessible via token)
 */
export interface ContractPublicView {
  contractId: string;
  contractNumber: string;
  status: string;
  propertyName: string;
  propertyAddress: string;
  ownerName: string;
  tenantName: string;
  rentalStartDate: string;
  rentalEndDate?: string;
  monthlyRent: string;
  createdAt: string;
  documentUrl?: string;
}

/**
 * Badge de statut pour l'affichage UI
 */
export interface SignatureStatusBadge {
  status: SignatureRequestStatus;
  label: string;
  color: 'primary' | 'success' | 'warning' | 'danger' | 'secondary' | 'info';
  icon: string;
}

/**
 * Helpers pour les statuts de signature
 */
export class SignatureStatusHelper {
  static getBadgeInfo(status: SignatureRequestStatus): SignatureStatusBadge {
    switch (status) {
      case SignatureRequestStatus.PENDING:
        return { status, label: 'En attente', color: 'secondary', icon: 'clock' };
      case SignatureRequestStatus.SENT:
        return { status, label: 'Envoyé', color: 'info', icon: 'envelope' };
      case SignatureRequestStatus.VIEWED:
        return { status, label: 'Consulté', color: 'primary', icon: 'eye' };
      case SignatureRequestStatus.SIGNED:
        return { status, label: 'Signé', color: 'success', icon: 'check-circle' };
      case SignatureRequestStatus.DECLINED:
        return { status, label: 'Refusé', color: 'danger', icon: 'x-circle' };
      case SignatureRequestStatus.CANCELLED:
        return { status, label: 'Annulé', color: 'warning', icon: 'ban' };
      case SignatureRequestStatus.EXPIRED:
        return { status, label: 'Expiré', color: 'danger', icon: 'clock' };
      default:
        return { status, label: 'Inconnu', color: 'secondary', icon: 'question' };
    }
  }

  static canResend(status: SignatureRequestStatus): boolean {
    return [
      SignatureRequestStatus.SENT,
      SignatureRequestStatus.VIEWED,
      SignatureRequestStatus.EXPIRED
    ].includes(status);
  }

  static canCancel(status: SignatureRequestStatus): boolean {
    return [
      SignatureRequestStatus.PENDING,
      SignatureRequestStatus.SENT,
      SignatureRequestStatus.VIEWED
    ].includes(status);
  }

  static isFinal(status: SignatureRequestStatus): boolean {
    return [
      SignatureRequestStatus.SIGNED,
      SignatureRequestStatus.DECLINED,
      SignatureRequestStatus.CANCELLED
    ].includes(status);
  }
}
