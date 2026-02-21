export interface FinancialDashboard {
  totalRevenue: number;
  totalExpenses: number;
  netIncome: number;
  pendingPayments: number;
  overduePayments: number;
  monthlyBreakdown: MonthlyBreakdown[];
  expensesByCategory: CategoryBreakdown[];
}

export interface MonthlyBreakdown {
  month: string;
  revenue: number;
  expenses: number;
  net: number;
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
}
