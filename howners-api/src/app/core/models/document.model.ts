export interface Document {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  mimeType: string;
  documentType: DocumentType;
  propertyId: string | null;
  rentalId: string | null;
  applicationId: string | null;
  uploaderId: string;
  uploaderName: string;
  documentHash: string;
  description: string | null;
  uploadedAt: string;
  retentionEndDate: string | null;
  archivedAt: string | null;
  isArchived: boolean;
  legalHold: boolean;
}

/**
 * Miroir de com.howners.gestion.domain.document.DocumentType.
 * Toute valeur renvoyée par l'API doit exister ici, sinon l'IHM affiche
 * la constante brute (ex. « MISE_EN_DEMEURE ») au lieu d'un libellé.
 */
export enum DocumentType {
  IDENTITY = 'IDENTITY',
  PROOF_OF_INCOME = 'PROOF_OF_INCOME',
  PROOF_OF_RESIDENCE = 'PROOF_OF_RESIDENCE',
  BANK_STATEMENT = 'BANK_STATEMENT',
  TAX_NOTICE = 'TAX_NOTICE',
  EMPLOYMENT_CONTRACT = 'EMPLOYMENT_CONTRACT',
  INVENTORY = 'INVENTORY',
  PHOTOS = 'PHOTOS',
  INVOICE = 'INVOICE',
  RECEIPT = 'RECEIPT',
  CONTRACT = 'CONTRACT',
  SIGNATURE = 'SIGNATURE',
  MISE_EN_DEMEURE = 'MISE_EN_DEMEURE',
  INSURANCE = 'INSURANCE',
  MAINTENANCE = 'MAINTENANCE',
  OTHER = 'OTHER',
  // Valeurs historiques du back, conservées pour les documents déjà stockés.
  ID_CARD = 'ID_CARD',
  PROOF_ADDRESS = 'PROOF_ADDRESS',
  PROOF_INCOME = 'PROOF_INCOME'
}

export const DOCUMENT_TYPE_LABELS: { [key in DocumentType]: string } = {
  [DocumentType.IDENTITY]: 'Pièce d\'identité',
  [DocumentType.PROOF_OF_INCOME]: 'Justificatif de revenus',
  [DocumentType.PROOF_OF_RESIDENCE]: 'Justificatif de domicile',
  [DocumentType.BANK_STATEMENT]: 'Relevé bancaire',
  [DocumentType.TAX_NOTICE]: 'Avis d\'imposition',
  [DocumentType.EMPLOYMENT_CONTRACT]: 'Contrat de travail',
  [DocumentType.INVENTORY]: 'État des lieux',
  [DocumentType.PHOTOS]: 'Photos',
  [DocumentType.INVOICE]: 'Facture',
  [DocumentType.RECEIPT]: 'Quittance',
  [DocumentType.CONTRACT]: 'Contrat',
  [DocumentType.SIGNATURE]: 'Signature',
  [DocumentType.MISE_EN_DEMEURE]: 'Mise en demeure',
  [DocumentType.INSURANCE]: 'Attestation d\'assurance',
  [DocumentType.MAINTENANCE]: 'Entretien chauffage',
  [DocumentType.OTHER]: 'Autre',
  [DocumentType.ID_CARD]: 'Pièce d\'identité',
  [DocumentType.PROOF_ADDRESS]: 'Justificatif de domicile',
  [DocumentType.PROOF_INCOME]: 'Justificatif de revenus'
};

/**
 * Types proposés à l'upload manuel : sans les doublons historiques ni les
 * documents produits par le back (signature, mise en demeure).
 */
export const UPLOADABLE_DOCUMENT_TYPES: DocumentType[] = [
  DocumentType.IDENTITY,
  DocumentType.PROOF_OF_INCOME,
  DocumentType.PROOF_OF_RESIDENCE,
  DocumentType.BANK_STATEMENT,
  DocumentType.TAX_NOTICE,
  DocumentType.EMPLOYMENT_CONTRACT,
  DocumentType.CONTRACT,
  DocumentType.INSURANCE,
  DocumentType.MAINTENANCE,
  DocumentType.INVENTORY,
  DocumentType.INVOICE,
  DocumentType.RECEIPT,
  DocumentType.PHOTOS,
  DocumentType.OTHER
];

export interface UploadDocumentRequest {
  file: File;
  documentType: DocumentType;
  propertyId?: string;
  rentalId?: string;
  applicationId?: string;
  description?: string;
}
