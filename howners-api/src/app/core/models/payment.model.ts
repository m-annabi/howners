export enum PaymentType {
  RENT = 'RENT',
  DEPOSIT = 'DEPOSIT',
  CHARGES = 'CHARGES',
  OTHER = 'OTHER'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  LATE = 'LATE',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
  CANCELLED = 'CANCELLED'
}

export interface Payment {
  id: string;
  rentalId: string;
  propertyName: string;
  payerId: string;
  payerName: string;
  paymentType: PaymentType;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: string;
  stripePaymentIntentId?: string;
  receiptUrl?: string;
  dueDate: string;
  paidAt?: string;
  createdAt: string;
}

export interface CreatePaymentRequest {
  rentalId: string;
  paymentType: PaymentType;
  amount: number;
  currency?: string;
  dueDate?: string;
  paymentMethod?: string;
}

export interface StripePaymentIntentResponse {
  clientSecret: string;
  paymentIntentId: string;
  status: string;
}

export const PAYMENT_TYPE_LABELS: { [key in PaymentType]: string } = {
  [PaymentType.RENT]: 'Loyer',
  [PaymentType.DEPOSIT]: 'Dépôt de garantie',
  [PaymentType.CHARGES]: 'Charges',
  [PaymentType.OTHER]: 'Autre'
};

export const PAYMENT_STATUS_LABELS: { [key in PaymentStatus]: string } = {
  [PaymentStatus.PENDING]: 'En attente',
  [PaymentStatus.PAID]: 'Payé',
  [PaymentStatus.LATE]: 'En retard',
  [PaymentStatus.FAILED]: 'Échoué',
  [PaymentStatus.REFUNDED]: 'Remboursé',
  [PaymentStatus.CANCELLED]: 'Annulé'
};

export const PAYMENT_STATUS_COLORS: { [key in PaymentStatus]: string } = {
  [PaymentStatus.PENDING]: 'warning',
  [PaymentStatus.PAID]: 'success',
  [PaymentStatus.LATE]: 'danger',
  [PaymentStatus.FAILED]: 'danger',
  [PaymentStatus.REFUNDED]: 'info',
  [PaymentStatus.CANCELLED]: 'secondary'
};
