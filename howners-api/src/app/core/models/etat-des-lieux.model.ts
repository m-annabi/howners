export interface EtatDesLieux {
  id: string;
  rentalId: string;
  propertyName: string;
  tenantName: string | null;
  type: EtatDesLieuxType;
  inspectionDate: string;
  roomConditions: string | null;
  meterReadings: string | null;
  keysCount: number | null;
  keysDescription: string | null;
  generalComments: string | null;
  ownerSigned: boolean;
  tenantSigned: boolean;
  ownerSignedAt: string | null;
  tenantSignedAt: string | null;
  createdByName: string;
  documentId: string | null;
  createdAt: string;
}

export enum EtatDesLieuxType {
  ENTREE = 'ENTREE',
  SORTIE = 'SORTIE'
}

export interface CreateEtatDesLieuxRequest {
  type: EtatDesLieuxType;
  inspectionDate: string;
  roomConditions?: string;
  meterReadings?: string;
  keysCount?: number;
  keysDescription?: string;
  generalComments?: string;
}

export const EDL_TYPE_LABELS: { [key in EtatDesLieuxType]: string } = {
  [EtatDesLieuxType.ENTREE]: 'Entrée',
  [EtatDesLieuxType.SORTIE]: 'Sortie'
};

export const EDL_TYPE_COLORS: { [key in EtatDesLieuxType]: string } = {
  [EtatDesLieuxType.ENTREE]: 'primary',
  [EtatDesLieuxType.SORTIE]: 'warning'
};
