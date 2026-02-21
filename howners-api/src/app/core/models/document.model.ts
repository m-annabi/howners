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
  OTHER = 'OTHER'
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
  [DocumentType.RECEIPT]: 'Reçu',
  [DocumentType.CONTRACT]: 'Contrat',
  [DocumentType.SIGNATURE]: 'Signature',
  [DocumentType.OTHER]: 'Autre'
};

export interface UploadDocumentRequest {
  file: File;
  documentType: DocumentType;
  propertyId?: string;
  rentalId?: string;
  applicationId?: string;
  description?: string;
}
