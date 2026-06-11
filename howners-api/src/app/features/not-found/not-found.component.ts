import { Component } from '@angular/core';

@Component({
  selector: 'app-not-found',
  template: `
    <div class="not-found-container">
      <div class="not-found-content">
        <h1 class="not-found-code">404</h1>
        <h2>Page introuvable</h2>
        <p class="text-muted">La page que vous recherchez n'existe pas ou a été déplacée.</p>
        <div class="not-found-actions">
          <a routerLink="/" class="btn btn-primary">
            <i class="bi bi-house me-2"></i>Retour à l'accueil
          </a>
          <a routerLink="/listings" class="btn btn-outline-secondary">
            <i class="bi bi-search me-2"></i>Parcourir les annonces
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .not-found-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 80vh;
      text-align: center;
      padding: 2rem;
    }
    .not-found-code {
      font-size: 6rem;
      font-weight: 700;
      color: #1E3A5F;
      margin-bottom: 0;
      line-height: 1;
    }
    h2 {
      font-size: 1.5rem;
      margin-bottom: 0.5rem;
    }
    .not-found-actions {
      margin-top: 1.5rem;
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
    }
  `]
})
export class NotFoundComponent {}
