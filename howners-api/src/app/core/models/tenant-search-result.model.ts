import { TenantSearchProfile } from './tenant-search-profile.model';
import { TenantScore } from './tenant-score.model';

export interface CompatibilityBreakdown {
  zoneScore: number;
  budgetScore: number;
  propertyTypeScore: number;
  surfaceScore: number;
  bedroomScore: number;
  furnishedScore: number;
}

export interface TenantSearchResult {
  profile: TenantSearchProfile;
  tenantScore?: TenantScore;
  compatibilityScore?: number;
  compatibility?: CompatibilityBreakdown;
}
