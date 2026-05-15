/**
 * E2E tests for the H1 + H2 growth features shipped tonight.
 * Uses mocked API responses (same pattern as the existing contracts suite).
 */
describe('H1 — Public landing page', () => {
  it('renders the landing on /', () => {
    cy.visit('/', { headers: { Accept: 'text/html' } });

    cy.contains('La gestion locative').should('be.visible');
    cy.contains('Commencer gratuitement').should('be.visible');
    cy.contains('Parcourir les annonces').should('be.visible');

    cy.contains('Un tarif clair').should('be.visible');
    cy.contains('Gratuit').should('be.visible');
    cy.contains('Pro').should('be.visible');
    cy.contains('Premium').should('be.visible');
    cy.contains('19,90 €').should('be.visible');

    cy.contains('Questions fréquentes').should('be.visible');
    cy.contains('opposables').should('be.visible');
  });

  it('redirects authenticated users to /dashboard', () => {
    cy.login();
    cy.intercept('GET', '**/api/dashboard/stats', { fixture: 'growth/dashboard-with-data.json' }).as('getStats');
    cy.intercept('GET', '**/api/feedback/nps/status', { body: { answered: true } });
    cy.intercept('GET', '**/api/payments', { body: [] });
    cy.intercept('GET', '**/api/contracts', { body: [] });
    cy.intercept('GET', '**/api/applications/received', { body: [] });
    cy.intercept('GET', '**/api/tenant-discovery*', { body: [] });

    cy.visit('/');
    cy.url({ timeout: 10000 }).should('include', '/dashboard');
  });
});

describe('H1 — Onboarding panel on empty dashboard', () => {
  function setupDashboardMocks() {
    cy.fixture('user.json').then((user) => {
      cy.intercept('GET', 'http://localhost:8080/api/auth/me', { statusCode: 200, body: user });
    });
    cy.intercept('GET', '**/api/messages/unread-count', { body: { count: 0 } });
    cy.intercept('GET', '**/api/payments', { body: [] });
    cy.intercept('GET', '**/api/contracts', { body: [] });
    cy.intercept('GET', '**/api/applications/received', { body: [] });
    cy.intercept('GET', '**/api/tenant-discovery*', { body: [] });
    cy.intercept('GET', '**/api/feedback/nps/status', { body: { answered: true } });
  }

  beforeEach(() => {
    setupDashboardMocks();
    cy.window().then(w => w.localStorage.setItem('access_token', 'fake-jwt-token-for-testing'));
  });

  it('shows the 3-step onboarding card for an owner with no property', () => {
    cy.intercept('GET', '**/api/dashboard/stats', { fixture: 'growth/dashboard-empty.json' }).as('getStats');

    cy.visit('/dashboard', {
      onBeforeLoad(win) {
        win.localStorage.setItem('access_token', 'fake-jwt-token-for-testing');
      },
    });
    cy.wait('@getStats');

    cy.contains('Trois minutes pour démarrer', { timeout: 10000 }).should('be.visible');
    cy.contains('Ajouter votre premier bien').should('be.visible');
    cy.contains('Créer une location').should('be.visible');
    cy.contains('Générer et envoyer le bail').should('be.visible');
  });

  it('hides the onboarding once the owner has properties', () => {
    cy.intercept('GET', '**/api/dashboard/stats', { fixture: 'growth/dashboard-with-data.json' }).as('getStats');

    cy.visit('/dashboard', {
      onBeforeLoad(win) {
        win.localStorage.setItem('access_token', 'fake-jwt-token-for-testing');
      },
    });
    cy.wait('@getStats');

    cy.contains('Trois minutes pour démarrer').should('not.exist');
  });
});

describe('H1 — Quota banner + upgrade modal on contracts', () => {
  beforeEach(() => {
    cy.login();
    cy.intercept('GET', '**/api/contracts', { body: [] });
    cy.intercept('GET', '**/api/feedback/nps/status', { body: { answered: true } });
  });

  it('shows the quota banner when the plan is saturated', () => {
    cy.intercept('GET', '**/api/subscriptions/usage', { fixture: 'growth/usage-saturated.json' }).as('getUsage');

    cy.visit('/contracts');
    cy.wait('@getUsage');

    cy.contains('Gratuit').should('be.visible');
    cy.contains('3 / 3 contrats').should('be.visible');
    cy.contains('Mettre à niveau').should('be.visible');
  });

  it('opens the upgrade modal when "Nouveau Contrat" is clicked at saturation', () => {
    cy.intercept('GET', '**/api/subscriptions/usage', { fixture: 'growth/usage-saturated.json' }).as('getUsage');

    cy.visit('/contracts');
    cy.wait('@getUsage');

    cy.get('button.hw-btn--primary').contains('Nouveau Contrat').click();

    cy.contains('Vous avez atteint votre quota mensuel').should('be.visible');
    cy.contains('Contrats illimités').should('be.visible');
    cy.contains('button', 'Voir les plans').should('be.visible');
  });

  it('does not show the upgrade modal when there is headroom', () => {
    cy.intercept('GET', '**/api/subscriptions/usage', { fixture: 'growth/usage-ok.json' }).as('getUsage');

    cy.visit('/contracts');
    cy.wait('@getUsage');

    cy.contains('Pro').should('be.visible');
    cy.contains('Vous avez atteint votre quota').should('not.exist');
  });
});

describe('H2 — Referral program', () => {
  beforeEach(() => {
    cy.login();
    cy.intercept('GET', '**/api/feedback/nps/status', { body: { answered: true } });
  });

  it('displays the user code + share URL + stats + invitee list', () => {
    cy.intercept('GET', '**/api/referrals/me', { fixture: 'growth/referral-summary.json' }).as('getReferral');

    cy.visit('/referral');
    cy.wait('@getReferral');

    cy.contains('Parrainez Howners', { timeout: 10000 }).should('be.visible');
    cy.contains('Votre code').should('be.visible');
    cy.contains('RCT8XNH').should('be.visible');
    cy.contains('http://localhost:4200/auth/register?ref=RCT8XNH').should('be.visible');

    cy.contains('Invitations en attente').should('be.visible');
    cy.contains('Inscriptions converties').should('be.visible');

    cy.contains('Léa Moreau').should('be.visible');
    cy.contains('Thomas Roux').should('be.visible');
    cy.contains('Camille Lefèvre').should('be.visible');
    cy.contains('Converti').should('be.visible');
    cy.contains('En attente').should('be.visible');
  });

  it('shows the empty state when nothing is shared yet', () => {
    cy.intercept('GET', '**/api/referrals/me', {
      body: {
        code: 'NEWUSER',
        shareUrl: 'http://localhost:4200/auth/register?ref=NEWUSER',
        pendingCount: 0,
        convertedCount: 0,
        referees: [],
      },
    }).as('getReferral');

    cy.visit('/referral');
    cy.wait('@getReferral');

    cy.contains('Personne pour le moment', { timeout: 10000 }).should('be.visible');
    cy.contains('NEWUSER').should('be.visible');
  });
});

describe('H1 — Register accepts ?ref= and shows the pill', () => {
  it('pre-fills the form and shows the invite pill', () => {
    cy.visit('/auth/register?ref=RCT8XNH', { headers: { Accept: 'text/html' } });
    cy.contains('Invité par un utilisateur Howners').should('be.visible');
    cy.contains('RCT8XNH').should('be.visible');
  });
});
