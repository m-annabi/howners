/// <reference types="cypress" />

// Teste l'espace locataire (features/tenant) contre la stack RÉELLE (backend :8080).
// Login UI réel avec un compte de seed, puis visite des 3 vues + captures d'écran.
// Une exception runtime d'un composant fait échouer le test (comportement Cypress par défaut).

describe('Espace locataire', () => {
  const EMAIL = 'tenant1@howners.test';
  const PASSWORD = 'Test1234!';

  beforeEach(() => {
    cy.loginWithCredentials(EMAIL, PASSWORD);
    // Attendre la fin du login réel : token stocké + redirection hors de /auth/login.
    cy.location('pathname', { timeout: 20000 }).should('not.include', '/auth/login');
    cy.window().its('localStorage.access_token').should('exist');
  });

  it('dashboard — le logement et les blocs locataire s’affichent', () => {
    cy.visit('/tenant/dashboard');
    cy.get('app-tenant-dashboard', { timeout: 15000 }).should('exist');
    // État valide quelle que soit la donnée : logement actif OU aucun logement.
    cy.contains(/Mon logement|Pas encore de location active/, { timeout: 15000 }).should('be.visible');
    cy.screenshot('tenant-1-dashboard', { capture: 'viewport' });
  });

  it('dossier — la pièce justificative se charge sans erreur', () => {
    cy.visit('/tenant/dossier');
    cy.get('app-tenant-dossier', { timeout: 15000 }).should('exist');
    // Le squelette de chargement doit disparaître…
    cy.get('app-loading-skeleton', { timeout: 15000 }).should('not.exist');
    // …et aucune alerte d'erreur (échec API ou upload) ne doit s'afficher.
    cy.get('.hw-alert--danger').should('not.exist');
    cy.screenshot('tenant-2-dossier', { capture: 'viewport' });
  });

  it('avis — la page des évaluations se charge (liste ou état vide)', () => {
    cy.visit('/tenant/avis');
    cy.get('app-tenant-avis', { timeout: 15000 }).should('exist');
    cy.get('app-loading-skeleton', { timeout: 15000 }).should('not.exist');
    cy.contains(/Aucun avis pour l’instant|Aucun avis pour l'instant|avis reçu/, { timeout: 15000 })
      .should('be.visible');
    cy.screenshot('tenant-3-avis', { capture: 'viewport' });
  });

  it('garde de rôle — un locataire n’accède pas à un espace propriétaire', () => {
    cy.visit('/properties');
    // RoleGuard doit rediriger hors de /properties (réservé OWNER/ADMIN).
    cy.location('pathname', { timeout: 15000 }).should('not.include', '/properties');
  });
});
