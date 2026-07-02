import { Component, OnInit } from '@angular/core';
import { AccountingService, AmortizableAsset, AssetSuggestion, FiscalActivity, LmnpResult, Loan } from '../../core/services/accounting.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-accounting',
  templateUrl: './accounting.component.html',
  styleUrls: ['./accounting.component.scss']
})
export class AccountingComponent implements OnInit {
  loading = true;
  activity: FiscalActivity | null = null;
  assets: AmortizableAsset[] = [];
  suggestions: AssetSuggestion[] = [];
  loans: Loan[] = [];
  result: LmnpResult | null = null;
  year = new Date().getFullYear() - 1;
  downloading = false;
  importing = false;

  // Formulaire activité
  activityForm = { startDate: '', openingCash: null as number | null };
  // Formulaire immobilisation
  assetTypes = [
    { value: 'BATIMENT', label: 'Immeuble (bâti)' },
    { value: 'MOBILIER', label: 'Mobilier et équipements' },
    { value: 'TRAVAUX', label: 'Travaux et agencements' },
    { value: 'FRAIS', label: "Frais d'acquisition" }
  ];
  assetForm = { type: 'MOBILIER', label: '', base: null as number | null, startDate: '', durationYears: null as number | null };
  // Formulaire emprunt
  loanForm = { label: '', principal: null as number | null, annualRate: null as number | null, durationMonths: null as number | null, startDate: '', insuranceMonthly: null as number | null };
  addingLoan = false;

  constructor(private accounting: AccountingService, private notify: NotificationService) {}

  ngOnInit(): void {
    this.load();
  }

  get chargePostes(): { poste: string; montant: number }[] {
    if (!this.result) return [];
    return Object.entries(this.result.chargesParPoste).map(([poste, montant]) => ({ poste, montant }));
  }

  get bilanEquilibre(): boolean {
    return !!this.result && Math.abs(this.result.totalActif - this.result.totalPassif) < 0.05;
  }

  load(): void {
    this.loading = true;
    this.accounting.getActivity().subscribe({
      next: (a) => {
        this.activity = a;
        if (a) {
          const startYear = new Date(a.startDate).getFullYear();
          if (this.year < startYear) {
            this.year = Math.max(startYear, new Date().getFullYear() - 1);
          }
          this.loadAssets();
          this.compute();
        } else {
          this.loading = false;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  loadAssets(): void {
    this.accounting.listAssets().subscribe(a => this.assets = a);
    this.accounting.suggestions().subscribe(s => this.suggestions = s);
    this.accounting.listLoans().subscribe(l => this.loans = l);
  }

  addLoan(): void {
    const f = this.loanForm;
    if (!f.principal || !f.annualRate || !f.durationMonths || !f.startDate) {
      this.notify.error('Capital, taux, durée et date de déblocage sont requis.'); return;
    }
    this.addingLoan = true;
    this.accounting.addLoan({
      label: f.label || undefined,
      principal: f.principal,
      annualRate: f.annualRate,
      durationMonths: f.durationMonths,
      startDate: f.startDate,
      insuranceMonthly: f.insuranceMonthly ?? undefined
    }).subscribe({
      next: () => {
        this.addingLoan = false;
        this.notify.success('Emprunt ajouté');
        this.loanForm = { label: '', principal: null, annualRate: null, durationMonths: null, startDate: '', insuranceMonthly: null };
        this.loadAssets(); this.compute();
      },
      error: (e) => { this.addingLoan = false; this.notify.error(e.error?.message || 'Erreur lors de l\'ajout'); }
    });
  }

  deleteLoan(l: Loan): void {
    if (!confirm(`Supprimer l'emprunt « ${l.label} » ?`)) return;
    this.accounting.deleteLoan(l.id).subscribe({
      next: () => { this.loadAssets(); this.compute(); },
      error: () => this.notify.error('Erreur lors de la suppression')
    });
  }

  importSuggestion(s: AssetSuggestion): void {
    this.importAll([s]);
  }

  importAllSuggestions(): void {
    this.importAll(this.suggestions);
  }

  private importAll(list: AssetSuggestion[]): void {
    if (list.length === 0) return;
    this.importing = true;
    const items = list.map(s => ({ sourceType: s.sourceType, sourceId: s.sourceId, durationYears: s.durationYears }));
    this.accounting.importSuggestions(items).subscribe({
      next: (created) => {
        this.importing = false;
        this.notify.success(`${created.length} immobilisation(s) importée(s)`);
        this.loadAssets();
        this.compute();
      },
      error: () => { this.importing = false; this.notify.error('Erreur lors de l\'import'); }
    });
  }

  saveActivity(): void {
    if (!this.activityForm.startDate) { this.notify.error('Renseignez la date de début d\'activité.'); return; }
    this.accounting.configureActivity({
      startDate: this.activityForm.startDate,
      openingCash: this.activityForm.openingCash ?? undefined
    }).subscribe({
      next: (a) => { this.activity = a; this.notify.success('Activité configurée'); this.loadAssets(); this.compute(); },
      error: () => this.notify.error('Erreur lors de la configuration')
    });
  }

  addAsset(): void {
    if (!this.assetForm.base || !this.assetForm.startDate) { this.notify.error('Base et date de mise en service requises.'); return; }
    this.accounting.addAsset({
      type: this.assetForm.type,
      label: this.assetForm.label || '',
      base: this.assetForm.base,
      startDate: this.assetForm.startDate,
      durationYears: this.assetForm.durationYears ?? undefined
    }).subscribe({
      next: () => {
        this.notify.success('Immobilisation ajoutée');
        this.assetForm = { type: 'MOBILIER', label: '', base: null, startDate: '', durationYears: null };
        this.loadAssets(); this.compute();
      },
      error: (e) => this.notify.error(e.error?.message || 'Erreur lors de l\'ajout')
    });
  }

  deleteAsset(a: AmortizableAsset): void {
    if (!confirm(`Supprimer « ${a.label} » ?`)) return;
    this.accounting.deleteAsset(a.id).subscribe({
      next: () => { this.loadAssets(); this.compute(); },
      error: () => this.notify.error('Erreur lors de la suppression')
    });
  }

  compute(): void {
    if (!this.activity) { this.loading = false; return; }
    this.loading = true;
    this.accounting.result(this.year).subscribe({
      next: (r) => { this.result = r; this.loading = false; },
      error: () => { this.result = null; this.loading = false; }
    });
  }

  downloadLiasse(): void {
    this.downloading = true;
    this.accounting.downloadLiasse(this.year).subscribe({
      next: (blob) => {
        this.downloading = false;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `liasse-lmnp-${this.year}.zip`; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.downloading = false; this.notify.error('Erreur lors du téléchargement'); }
    });
  }
}
