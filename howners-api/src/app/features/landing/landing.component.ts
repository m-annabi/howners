import { Component, OnInit, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit {
  isAuthenticated = false;
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
    private seoService: SeoService,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit(): void {
    // Si déjà authentifié, rediriger vers l'accueil DU RÔLE : un locataire va dans
    // son espace, pas sur le tableau de bord propriétaire (gestion des biens).
    // « Voir le site » depuis le menu (?site=1) : on affiche la page sans rediriger.
    this.isAuthenticated = this.auth.isAuthenticated();
    if (this.isAuthenticated && !this.route.snapshot.queryParamMap.has('site')) {
      this.auth.resolveHomePath().subscribe(path => this.router.navigate([path]));
      return;
    }

    const title = 'Howners — Gestion locative simplifiée pour propriétaires';
    const description =
      'Gérez vos biens, contrats, paiements et quittances en un seul endroit. ' +
      'Signature électronique, suivi des loyers, fiches locataires — sans tableurs.';

    // URL canonique de prod (pas window.location.origin : faux en dev, indéfini en prerender).
    this.seoService.setMetaTags({ title, description, url: 'https://howners.com/', type: 'website' });
    this.seoService.setCanonical('https://howners.com/');

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

  /** Utilisateur connecté : retour à son espace (tableau de bord ou espace locataire). */
  goToSpace(): void {
    this.auth.resolveHomePath().subscribe(path => this.router.navigate([path]));
  }

  private injectJsonLd(): void {
    const existing = this.document.getElementById('howners-jsonld');
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

    const script = this.document.createElement('script');
    script.id = 'howners-jsonld';
    script.type = 'application/ld+json';
    script.text = JSON.stringify(ld);
    this.document.head.appendChild(script);
  }
}
