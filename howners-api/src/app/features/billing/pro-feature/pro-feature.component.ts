import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { PlanName } from '../../../core/models/subscription.model';

interface ProFeatureDetail {
  icon: string;
  title: string;
  text: string;
}

interface ProFeatureConfig {
  icon: string;
  title: string;
  headline: string;
  tagline: string;
  featureRoute: string;
  details: ProFeatureDetail[];
}

const PRO_FEATURES: { [key: string]: ProFeatureConfig } = {
  'patrimoine': {
    icon: 'bi-pie-chart',
    title: 'Patrimoine',
    headline: 'Pilotez votre patrimoine immobilier en un coup d\'œil',
    tagline: 'Un dashboard patrimonial complet qui consolide la valeur, les revenus et la rentabilité de tous vos biens.',
    featureRoute: '/financial/patrimoine',
    details: [
      {
        icon: 'bi-buildings',
        title: 'Vue consolidée du portefeuille',
        text: 'Valeur d\'achat totale de vos biens et détail bien par bien : prix d\'achat, loyers, charges.'
      },
      {
        icon: 'bi-cash-stack',
        title: 'Revenus et cash-flow',
        text: 'Revenus des 12 derniers mois et cash-flow mensuel consolidé, positif ou négatif, en temps réel.'
      },
      {
        icon: 'bi-percent',
        title: 'Rendement net',
        text: 'Rendement net moyen pondéré de votre portefeuille et rentabilité calculée pour chaque bien.'
      },
      {
        icon: 'bi-graph-up-arrow',
        title: 'Suivi dans le temps',
        text: 'Suivez l\'évolution de votre patrimoine au fil des locations, des dépenses et des travaux.'
      }
    ]
  },
  'comptabilite': {
    icon: 'bi-calculator',
    title: 'Comptabilité LMNP',
    headline: 'Votre comptabilité LMNP au réel, sans expert-comptable',
    tagline: 'Écritures, amortissements, bilan et liasse fiscale générés automatiquement à partir de vos loyers et dépenses.',
    featureRoute: '/accounting',
    details: [
      {
        icon: 'bi-journal-check',
        title: 'Écritures automatiques',
        text: 'Les écritures comptables sont générées automatiquement depuis vos paiements et vos dépenses.'
      },
      {
        icon: 'bi-house-gear',
        title: 'Amortissements calculés',
        text: 'Amortissement des biens, du mobilier et des frais d\'acquisition calculé composant par composant.'
      },
      {
        icon: 'bi-bank',
        title: 'Prêts et intérêts',
        text: 'Échéanciers de prêt intégrés : les intérêts sont ventilés et déduits automatiquement chaque exercice.'
      },
      {
        icon: 'bi-file-earmark-zip',
        title: 'Liasse fiscale et FEC',
        text: 'Bilan, compte de résultat, liasse 2031 et export FEC réglementaire téléchargeables en un clic.'
      }
    ]
  },
  'fiscal-2044': {
    icon: 'bi-file-earmark-spreadsheet',
    title: 'Export fiscal',
    headline: 'Votre déclaration 2044 pré-remplie',
    tagline: 'Fini les calculs à la main : vos revenus fonciers sont ventilés ligne par ligne, prêts à reporter sur votre déclaration.',
    featureRoute: '/financial/fiscal-2044',
    details: [
      {
        icon: 'bi-list-check',
        title: 'Ventilation ligne par ligne',
        text: 'Revenus bruts (ligne 211), charges déductibles, intérêts d\'emprunt : chaque montant est calculé et rattaché à sa ligne 2044.'
      },
      {
        icon: 'bi-building',
        title: 'Détail par bien',
        text: 'Récapitulatif annuel des revenus et charges pour chacun de vos biens, sur l\'année fiscale de votre choix.'
      },
      {
        icon: 'bi-file-earmark-pdf',
        title: 'PDF prêt à l\'emploi',
        text: 'Téléchargez un PDF clair à reporter directement sur votre déclaration de revenus fonciers.'
      },
      {
        icon: 'bi-filetype-csv',
        title: 'Export CSV',
        text: 'Exportez le détail en CSV pour vos archives ou pour votre comptable.'
      }
    ]
  }
};

@Component({
  selector: 'app-pro-feature',
  templateUrl: './pro-feature.component.html',
  styleUrls: ['./pro-feature.component.scss']
})
export class ProFeatureComponent implements OnInit {
  config: ProFeatureConfig | null = null;
  proPrice: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private subscriptionService: SubscriptionService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const key = params.get('feature') || '';
      this.config = PRO_FEATURES[key] || null;
      if (!this.config) {
        this.router.navigate(['/billing/pricing']);
        return;
      }
      this.redirectIfAlreadyPro();
    });

    this.subscriptionService.getPlans().subscribe({
      next: (plans) => {
        const pro = plans.find(p => p.name === PlanName.PRO);
        this.proPrice = pro ? pro.monthlyPrice : null;
      },
      error: () => {
        this.proPrice = null;
      }
    });
  }

  private redirectIfAlreadyPro(): void {
    this.subscriptionService.getUsageLimits().subscribe({
      next: (usage) => {
        if (this.config && (usage.planName || 'FREE') !== 'FREE') {
          this.router.navigateByUrl(this.config.featureRoute);
        }
      },
      error: () => { /* on reste sur la page de présentation */ }
    });
  }
}
