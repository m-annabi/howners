import { Injectable, Inject } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

/**
 * Service centralisé pour la gestion du SEO : titre, méta-tags, URL canonique.
 * Utilise les services Angular Title et Meta de @angular/platform-browser.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {

  private readonly defaultImage = 'https://howners.fr/assets/og-howners.png';
  private readonly siteName = 'Howners';

  constructor(
    private titleService: Title,
    private meta: Meta,
    @Inject(DOCUMENT) private document: Document
  ) {}

  /**
   * Met à jour le titre de la page.
   */
  setTitle(title: string): void {
    this.titleService.setTitle(title);
  }

  /**
   * Met à jour l'ensemble des méta-tags SEO + Open Graph + Twitter Card.
   */
  setMetaTags(config: {
    title: string;
    description: string;
    url?: string;
    image?: string;
    type?: string;
  }): void {
    const { title, description, type = 'website' } = config;
    const url = config.url || (typeof window !== 'undefined' ? window.location.href : '');
    const image = config.image || this.defaultImage;

    // Titre
    this.titleService.setTitle(title);

    // Méta classiques
    this.meta.updateTag({ name: 'description', content: description });

    // Open Graph
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: type });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ property: 'og:site_name', content: this.siteName });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    // Twitter Card
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: description });
    this.meta.updateTag({ name: 'twitter:image', content: image });
  }

  /**
   * Met à jour la balise <link rel="canonical"> dans le <head>.
   */
  setCanonical(url: string): void {
    let link: HTMLLinkElement | null = this.document.querySelector('link[rel="canonical"]');
    if (link) {
      link.setAttribute('href', url);
    } else {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      link.setAttribute('href', url);
      this.document.head.appendChild(link);
    }
  }
}
