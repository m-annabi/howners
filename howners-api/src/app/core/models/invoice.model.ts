export enum InvoiceType {
  RENT = 'RENT',
  DEPOSIT = 'DEPOSIT',
  CHARGES = 'CHARGES',
  OTHER = 'OTHER'
}

export enum InvoiceStatus {
  DRAFT = 'DRAFT',
  ISSUED = 'ISSUED',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED'
}

export interface Invoice {
  id: string;
  rentalId: string;
  propertyName: string;
  tenantName: string;
  invoiceNumber: string;
  invoiceType: InvoiceType;
  amount: number;
  currency: string;
  issueDate: string;
  dueDate: string;
  status: InvoiceStatus;
  paymentId?: string;
  documentId?: string;
  createdAt: string;
}

export interface CreateInvoiceRequest {
  rentalId: string;
  invoiceType: InvoiceType;
  amount: number;
  issueDate: string;
  dueDate?: string;
}

export const INVOICE_TYPE_LABELS: { [key in InvoiceType]: string } = {
  [InvoiceType.RENT]: 'Loyer',
  [InvoiceType.DEPOSIT]: 'Dépôt',
  [InvoiceType.CHARGES]: 'Charges',
  [InvoiceType.OTHER]: 'Autre'
};

export const INVOICE_STATUS_LABELS: { [key in InvoiceStatus]: string } = {
  [InvoiceStatus.DRAFT]: 'Brouillon',
  [InvoiceStatus.ISSUED]: 'Émise',
  [InvoiceStatus.PAID]: 'Payée',
  [InvoiceStatus.OVERDUE]: 'En retard',
  [InvoiceStatus.CANCELLED]: 'Annulée'
};

export const INVOICE_STATUS_COLORS: { [key in InvoiceStatus]: string } = {
  [InvoiceStatus.DRAFT]: 'secondary',
  [InvoiceStatus.ISSUED]: 'primary',
  [InvoiceStatus.PAID]: 'success',
  [InvoiceStatus.OVERDUE]: 'danger',
  [InvoiceStatus.CANCELLED]: 'secondary'
};
