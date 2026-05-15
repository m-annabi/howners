import { Component, OnInit } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit {
  constructor(
    private titleService: Title,
    private meta: Meta,
    private router: Router,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    // If already authenticated, send them to the dashboard.
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    const title = 'Howners — Gestion locative simple et professionnelle';
    const description = 'Gérez vos biens, contrats, paiements et quittances en un seul endroit. Signature électronique, suivi des loyers, fiches locataires — sans tableurs.';

    this.titleService.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });

    // Open Graph
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:url', content: window.location.origin });
    this.meta.updateTag({ property: 'og:site_name', content: 'Howners' });

    // Twitter
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: description });

    // Robots
    this.meta.updateTag({ name: 'robots', content: 'index, follow' });

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
