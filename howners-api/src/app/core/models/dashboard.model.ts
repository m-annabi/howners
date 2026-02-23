import { Property } from './property.model';
import { Rental } from './rental.model';

export interface TenantInfo {
  totalApplications: number;
  pendingApplications: number;
  pendingInvitations: number;
  totalInvitations: number;
  searchProfileActive: boolean;
  unreadMessages: number;
}

export interface DashboardStats {
  totalProperties: number;
  activeRentals: number;
  pendingRentals: number;
  terminatedRentals: number;
  monthlyRevenue: number;
  currency: string;
  recentActivity: {
    latestProperty: Property | null;
    latestRental: Rental | null;
  };
  tenantInfo?: TenantInfo;
}
