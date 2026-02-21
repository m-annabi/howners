export enum ConsentType {
  DATA_PROCESSING = 'DATA_PROCESSING',
  MARKETING_EMAILS = 'MARKETING_EMAILS',
  ANALYTICS = 'ANALYTICS',
  THIRD_PARTY_SHARING = 'THIRD_PARTY_SHARING'
}

export interface ConsentResponse {
  id: string;
  consentType: ConsentType;
  granted: boolean;
  grantedAt: string;
  revokedAt: string;
  updatedAt: string;
}

export interface ConsentRequest {
  consentType: ConsentType;
  granted: boolean;
}

export interface UserDataExport {
  exportDate: string;
  personalInfo: {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    role: string;
    createdAt: string;
  };
  properties: any[];
  rentals: any[];
  contracts: any[];
  payments: any[];
  documents: any[];
  consents: ConsentResponse[];
}

export const CONSENT_TYPE_LABELS: { [key in ConsentType]: string } = {
  [ConsentType.DATA_PROCESSING]: 'Traitement des données',
  [ConsentType.MARKETING_EMAILS]: 'Emails marketing',
  [ConsentType.ANALYTICS]: 'Analyse d\'utilisation',
  [ConsentType.THIRD_PARTY_SHARING]: 'Partage avec des tiers'
};
