export enum PropertyType {
  APARTMENT = 'APARTMENT',
  HOUSE = 'HOUSE',
  STUDIO = 'STUDIO',
  DUPLEX = 'DUPLEX',
  VILLA = 'VILLA',
  LOFT = 'LOFT',
  OTHER = 'OTHER'
}

export enum HeatingType {
  INDIVIDUAL_GAS = 'INDIVIDUAL_GAS',
  COLLECTIVE_GAS = 'COLLECTIVE_GAS',
  INDIVIDUAL_ELECTRIC = 'INDIVIDUAL_ELECTRIC',
  COLLECTIVE_ELECTRIC = 'COLLECTIVE_ELECTRIC',
  HEAT_PUMP = 'HEAT_PUMP',
  FUEL_OIL = 'FUEL_OIL',
  WOOD = 'WOOD',
  DISTRICT_HEATING = 'DISTRICT_HEATING',
  OTHER = 'OTHER',
  NONE = 'NONE'
}

export enum PropertyCondition {
  NEW = 'NEW',
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  TO_REFRESH = 'TO_REFRESH',
  TO_RENOVATE = 'TO_RENOVATE'
}

export interface Address {
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode: string;
  department?: string;
  country: string;
}

export interface Property {
  id: string;
  ownerId: string;
  name: string;
  propertyType: PropertyType;
  address: Address;
  surfaceArea?: number;
  bedrooms?: number;
  bathrooms?: number;
  description?: string;
  condoFees?: number;
  propertyTax?: number;
  businessTax?: number;
  homeInsurance?: number;
  purchasePrice?: number;
  dpeRating?: string;
  gesRating?: string;
  constructionYear?: number;
  floorNumber?: number;
  totalFloors?: number;
  heatingType?: HeatingType;
  hasParking?: boolean;
  hasElevator?: boolean;
  isFurnished?: boolean;
  propertyCondition?: PropertyCondition;
  primaryPhotoUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePropertyRequest {
  name: string;
  propertyType: PropertyType;
  address: Address;
  surfaceArea?: number;
  bedrooms?: number;
  bathrooms?: number;
  description?: string;
  condoFees?: number;
  propertyTax?: number;
  businessTax?: number;
  homeInsurance?: number;
  purchasePrice?: number;
  dpeRating?: string;
  gesRating?: string;
  constructionYear?: number;
  floorNumber?: number;
  totalFloors?: number;
  heatingType?: HeatingType;
  hasParking?: boolean;
  hasElevator?: boolean;
  isFurnished?: boolean;
  propertyCondition?: PropertyCondition;
}

export interface UpdatePropertyRequest {
  name?: string;
  propertyType?: PropertyType;
  address?: Address;
  surfaceArea?: number;
  bedrooms?: number;
  bathrooms?: number;
  description?: string;
  condoFees?: number;
  propertyTax?: number;
  businessTax?: number;
  homeInsurance?: number;
  purchasePrice?: number;
  dpeRating?: string;
  gesRating?: string;
  constructionYear?: number;
  floorNumber?: number;
  totalFloors?: number;
  heatingType?: HeatingType;
  hasParking?: boolean;
  hasElevator?: boolean;
  isFurnished?: boolean;
  propertyCondition?: PropertyCondition;
}

export const PROPERTY_TYPE_LABELS: Record<PropertyType, string> = {
  [PropertyType.APARTMENT]: 'Appartement',
  [PropertyType.HOUSE]: 'Maison',
  [PropertyType.STUDIO]: 'Studio',
  [PropertyType.DUPLEX]: 'Duplex',
  [PropertyType.VILLA]: 'Villa',
  [PropertyType.LOFT]: 'Loft',
  [PropertyType.OTHER]: 'Autre'
};

export const HEATING_TYPE_LABELS: Record<HeatingType, string> = {
  [HeatingType.INDIVIDUAL_GAS]: 'Gaz individuel',
  [HeatingType.COLLECTIVE_GAS]: 'Gaz collectif',
  [HeatingType.INDIVIDUAL_ELECTRIC]: 'Électrique individuel',
  [HeatingType.COLLECTIVE_ELECTRIC]: 'Électrique collectif',
  [HeatingType.HEAT_PUMP]: 'Pompe à chaleur',
  [HeatingType.FUEL_OIL]: 'Fioul',
  [HeatingType.WOOD]: 'Bois',
  [HeatingType.DISTRICT_HEATING]: 'Chauffage urbain',
  [HeatingType.OTHER]: 'Autre',
  [HeatingType.NONE]: 'Aucun'
};

export const PROPERTY_CONDITION_LABELS: Record<PropertyCondition, string> = {
  [PropertyCondition.NEW]: 'Neuf',
  [PropertyCondition.EXCELLENT]: 'Excellent état',
  [PropertyCondition.GOOD]: 'Bon état',
  [PropertyCondition.TO_REFRESH]: 'À rafraîchir',
  [PropertyCondition.TO_RENOVATE]: 'À rénover'
};

export const DPE_COLORS: Record<string, string> = {
  'A': '#319834',
  'B': '#34a933',
  'C': '#cfdb2d',
  'D': '#fbdb33',
  'E': '#f5ac1c',
  'F': '#ee7e23',
  'G': '#e9413e'
};

export const GES_COLORS: Record<string, string> = {
  'A': '#fbfcfd',
  'B': '#d7d1e9',
  'C': '#cab9df',
  'D': '#b48fc6',
  'E': '#a075b9',
  'F': '#8e5ea6',
  'G': '#6b3c7c'
};
