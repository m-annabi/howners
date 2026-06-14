import { Component, OnInit } from '@angular/core';
import { Patrimoine, PatrimoineService } from '../../../core/services/patrimoine.service';

@Component({
  selector: 'app-patrimoine',
  templateUrl: './patrimoine.component.html',
  styleUrls: ['./patrimoine.component.scss']
})
export class PatrimoineComponent implements OnInit {
  patrimoine: Patrimoine | null = null;
  loading = false;
  error: string | null = null;

  constructor(private patrimoineService: PatrimoineService) {}

  ngOnInit(): void {
    this.loading = true;
    this.patrimoineService.getPatrimoine().subscribe({
      next: (patrimoine) => {
        this.patrimoine = patrimoine;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du chargement du patrimoine.';
        this.loading = false;
      }
    });
  }

  getCashFlowBarWidth(bien: { cashFlowMensuel: number }): number {
    if (!this.patrimoine) return 0;
    const max = Math.max(...this.patrimoine.biens.map(b => Math.abs(b.cashFlowMensuel)), 1);
    return Math.round((Math.abs(bien.cashFlowMensuel) / max) * 100);
  }
}
