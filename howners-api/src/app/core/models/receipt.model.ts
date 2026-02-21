export interface Receipt {
  id: string;
  rentalId: string;
  propertyName: string;
  tenantName: string;
  ownerName: string;
  receiptNumber: string;
  periodStart: string;
  periodEnd: string;
  amount: number;
  currency: string;
  paymentId: string;
  documentId?: string;
  documentUrl?: string;
  createdAt: string;
}
