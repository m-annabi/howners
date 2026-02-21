export interface ContractTemplate {
  id: string;
  name: string;
  description: string | null;
  rentalType: RentalType;
  content: string;
  isDefault: boolean;
  isActive: boolean;
  createdById: string | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export enum RentalType {
  LONG_TERM = 'LONG_TERM',
  SHORT_TERM = 'SHORT_TERM'
}

export interface CreateTemplateRequest {
  name: string;
  description?: string;
  rentalType: RentalType;
  content: string;
}

export interface UpdateTemplateRequest {
  name?: string;
  description?: string;
  content?: string;
  isActive?: boolean;
}

export interface PreviewTemplateRequest {
  rentalId: string;
  customContent?: string;
}

export interface PreviewTemplateResponse {
  filledContent: string;
  rentalPropertyName: string;
  tenantFullName: string;
}

export interface TemplateVariable {
  key: string;
  label: string;
  category: string;
  example: string;
}

export interface TemplateVariablesResponse {
  variables: TemplateVariable[];
}

export const RENTAL_TYPE_LABELS: { [key in RentalType]: string } = {
  [RentalType.LONG_TERM]: 'Location longue durée',
  [RentalType.SHORT_TERM]: 'Location courte durée'
};

export const RENTAL_TYPE_ICONS: { [key in RentalType]: string } = {
  [RentalType.LONG_TERM]: 'bi-calendar-check',
  [RentalType.SHORT_TERM]: 'bi-calendar-week'
};

export const VARIABLE_CATEGORY_LABELS: { [key: string]: string } = {
  'owner': 'Propriétaire',
  'tenant': 'Locataire',
  'property': 'Propriété',
  'rental': 'Location',
  'date': 'Date'
};

export const VARIABLE_CATEGORY_COLORS: { [key: string]: string } = {
  'owner': '#0d6efd',    // Blue
  'tenant': '#198754',   // Green
  'property': '#6f42c1', // Purple
  'rental': '#fd7e14',   // Orange
  'date': '#6c757d'      // Gray
};
