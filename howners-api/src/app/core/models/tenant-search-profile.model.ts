import { PropertyType } from './property.model';

export enum FurnishedPreference {
  FURNISHED_ONLY = 'FURNISHED_ONLY',
  UNFURNISHED_ONLY = 'UNFURNISHED_ONLY',
  NO_PREFERENCE = 'NO_PREFERENCE'
}

export const FURNISHED_PREFERENCE_LABELS: Record<FurnishedPreference, string> = {
  [FurnishedPreference.FURNISHED_ONLY]: 'Meublé uniquement',
  [FurnishedPreference.UNFURNISHED_ONLY]: 'Non meublé uniquement',
  [FurnishedPreference.NO_PREFERENCE]: 'Indifférent'
};

export interface TenantSearchProfile {
  id: string;
  tenantId: string;
  tenantName: string;
  tenantEmail: string;
  desiredCity?: string;
  desiredDepartment?: string;
  desiredPostalCode?: string;
  budgetMin?: number;
  budgetMax?: number;
  desiredPropertyType?: PropertyType;
  minSurface?: number;
  minBedrooms?: number;
  furnishedPreference: FurnishedPreference;
  desiredMoveIn?: string;
  description?: string;
  isActive: boolean;
  updatedAt: string;
}

export interface CreateTenantSearchProfileRequest {
  desiredCity?: string;
  desiredDepartment?: string;
  desiredPostalCode?: string;
  budgetMin?: number;
  budgetMax?: number;
  desiredPropertyType?: PropertyType;
  minSurface?: number;
  minBedrooms?: number;
  furnishedPreference?: FurnishedPreference;
  desiredMoveIn?: string;
  description?: string;
}
