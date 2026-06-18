import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit {
  constructor(
    private router: Router,
    private auth: AuthService,
    private seoService: SeoService
  ) {}

  ngOnInit(): void {
    // Si déjà authentifié, rediriger vers le tableau de bord.
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    const title = 'Howners — Gestion locative simplifiée pour propriétaires';
    const description =
      'Gérez vos biens, contrats, paiements et quittances en un seul endroit. ' +
      'Signature électronique, suivi des loyers, fiches locataires — sans tableurs.';

    // URL canonique de prod (pas window.location.origin : faux en dev, indéfini en prerender).
    this.seoService.setMetaTags({ title, description, url: 'https://howners.fr/', type: 'website' });
    this.seoService.setCanonical('https://howners.fr/');

    // schema.org SoftwareApplication
    this.injectJsonLd();
  }

  signup(): void {
    this.router.navigate(['/auth/register']);
  }

  login(): void {
    this.router.navigate(['/auth/login']);
  }

  browseListings(): void {
    this.router.navigate(['/listings']);
  }

  private injectJsonLd(): void {
    const existing = document.getElementById('howners-jsonld');
    if (existing) existing.remove();

    const ld = {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: 'Howners',
      applicationCategory: 'BusinessApplication',
      operatingSystem: 'Web',
      description: 'Plateforme de gestion locative pour propriétaires : contrats, signature électronique, suivi des loyers, fiches locataires.',
      offers: [
        {
          '@type': 'Offer',
          name: 'Plan Gratuit',
          price: '0',
          priceCurrency: 'EUR'
        },
        {
          '@type': 'Offer',
          name: 'Plan Pro',
          price: '19.90',
          priceCurrency: 'EUR'
        }
      ]
    };

    const script = document.createElement('script');
    script.id = 'howners-jsonld';
    script.type = 'application/ld+json';
    script.text = JSON.stringify(ld);
    document.head.appendChild(script);
  }
}
