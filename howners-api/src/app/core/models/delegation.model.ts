import { FinancialDashboard } from './financial-dashboard.model';

export interface Delegation {
  id: string;
  agenceUserId: string;
  agenceNom: string;
  agenceEmail: string;
  delegueUserId: string;
  delegueNom: string;
  delegueEmail: string;
  statut: 'ACTIVE' | 'REVOQUEE';
  createdAt: string;
}

export interface ApercuCompte {
  agenceUserId: string;
  agenceNom: string;
  agenceEmail: string;
  totalProperties: number;
  activeRentals: number;
  finances: FinancialDashboard;
}
