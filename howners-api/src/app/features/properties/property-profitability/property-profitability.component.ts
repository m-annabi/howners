import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Property } from '../../../core/models/property.model';

interface ProfitabilityMetrics {
  // Revenus
  monthlyRent: number;
  annualIncome: number;

  // Charges mensuelles et annuelles
  monthlyCondo: number;
  monthlyCharges: number;
  annualCharges: number;

  // Revenus nets
  netMonthlyIncome: number;
  netAnnualIncome: number;

  // Rentabilité
  grossYield: number; // Rentabilité brute
  netYield: number;   // Rentabilité nette

  // Cash-flow
  monthlyCashFlow: number;
  annualCashFlow: number;

  // Indicateurs
  hasAllData: boolean;
  hasPurchasePrice: boolean;
  hasRentalData: boolean;
}

@Component({
  selector: 'app-property-profitability',
  templateUrl: './property-profitability.component.html',
  styleUrls: ['./property-profitability.component.scss']
})
export class PropertyProfitabilityComponent implements OnInit, OnChanges {
  @Input() property: Property | null = null;
  @Input() monthlyRent: number = 0; // Loyer actuel de la location

  metrics: ProfitabilityMetrics | null = null;

  ngOnInit(): void {
    this.calculateMetrics();
  }

  ngOnChanges(): void {
    this.calculateMetrics();
  }

  calculateMetrics(): void {
    if (!this.property) {
      this.metrics = null;
      return;
    }

    const condoFees = this.property.condoFees || 0;
    const propertyTax = this.property.propertyTax || 0;
    const businessTax = this.property.businessTax || 0;
    const homeInsurance = this.property.homeInsurance || 0;
    const purchasePrice = this.property.purchasePrice || 0;
    const monthlyRent = this.monthlyRent || 0;

    // Calculs
    const annualIncome = monthlyRent * 12;
    const monthlyCondo = condoFees;
    const monthlyCharges = condoFees;
    const annualCharges = (condoFees * 12) + propertyTax + businessTax + homeInsurance;

    const netMonthlyIncome = monthlyRent - monthlyCharges;
    const netAnnualIncome = annualIncome - annualCharges;

    const grossYield = purchasePrice > 0 ? (annualIncome / purchasePrice) * 100 : 0;
    const netYield = purchasePrice > 0 ? (netAnnualIncome / purchasePrice) * 100 : 0;

    const monthlyCashFlow = netMonthlyIncome;
    const annualCashFlow = netAnnualIncome;

    this.metrics = {
      monthlyRent,
      annualIncome,
      monthlyCondo,
      monthlyCharges,
      annualCharges,
      netMonthlyIncome,
      netAnnualIncome,
      grossYield,
      netYield,
      monthlyCashFlow,
      annualCashFlow,
      hasAllData: purchasePrice > 0 && monthlyRent > 0,
      hasPurchasePrice: purchasePrice > 0,
      hasRentalData: monthlyRent > 0
    };
  }

  getYieldColor(yield_: number): string {
    if (yield_ >= 5) return 'text-success';
    if (yield_ >= 3) return 'text-warning';
    return 'text-danger';
  }

  getCashFlowColor(cashFlow: number): string {
    if (cashFlow > 0) return 'text-success';
    if (cashFlow === 0) return 'text-warning';
    return 'text-danger';
  }
}
