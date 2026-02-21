import { Property } from './property.model';
import { Rental } from './rental.model';

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
}
