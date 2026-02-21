export enum ExpenseCategory {
  MAINTENANCE = 'MAINTENANCE',
  REPAIR = 'REPAIR',
  INSURANCE = 'INSURANCE',
  TAX = 'TAX',
  CONDO_FEES = 'CONDO_FEES',
  UTILITIES = 'UTILITIES',
  MANAGEMENT_FEES = 'MANAGEMENT_FEES',
  LEGAL = 'LEGAL',
  RENOVATION = 'RENOVATION',
  FURNISHING = 'FURNISHING',
  CLEANING = 'CLEANING',
  OTHER = 'OTHER'
}

export interface Expense {
  id: string;
  propertyId?: string;
  propertyName?: string;
  rentalId?: string;
  category: ExpenseCategory;
  description?: string;
  amount: number;
  currency: string;
  expenseDate: string;
  documentId?: string;
  createdAt: string;
}

export interface CreateExpenseRequest {
  propertyId?: string;
  rentalId?: string;
  category: ExpenseCategory;
  description?: string;
  amount: number;
  expenseDate: string;
}

export interface UpdateExpenseRequest {
  propertyId?: string;
  rentalId?: string;
  category?: ExpenseCategory;
  description?: string;
  amount?: number;
  expenseDate?: string;
}

export const EXPENSE_CATEGORY_LABELS: { [key in ExpenseCategory]: string } = {
  [ExpenseCategory.MAINTENANCE]: 'Entretien',
  [ExpenseCategory.REPAIR]: 'Réparation',
  [ExpenseCategory.INSURANCE]: 'Assurance',
  [ExpenseCategory.TAX]: 'Impôts/Taxes',
  [ExpenseCategory.CONDO_FEES]: 'Charges copropriété',
  [ExpenseCategory.UTILITIES]: 'Services',
  [ExpenseCategory.MANAGEMENT_FEES]: 'Frais de gestion',
  [ExpenseCategory.LEGAL]: 'Frais juridiques',
  [ExpenseCategory.RENOVATION]: 'Travaux/Rénovation',
  [ExpenseCategory.FURNISHING]: 'Ameublement',
  [ExpenseCategory.CLEANING]: 'Nettoyage',
  [ExpenseCategory.OTHER]: 'Autre'
};
