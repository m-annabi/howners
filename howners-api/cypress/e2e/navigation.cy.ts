describe('Navigation Guards', () => {
  beforeEach(() => {
    // Clear any stored auth token to simulate an unauthenticated user
    cy.clearLocalStorage();
  });

  describe('Protected routes (require auth)', () => {
    it('should redirect /dashboard to /auth/login when not authenticated', () => {
      cy.visit('/dashboard');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /properties to /auth/login when not authenticated', () => {
      cy.visit('/properties');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /contracts to /auth/login when not authenticated', () => {
      cy.visit('/contracts');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /rentals to /auth/login when not authenticated', () => {
      cy.visit('/rentals');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /templates to /auth/login when not authenticated', () => {
      cy.visit('/templates');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /profile to /auth/login when not authenticated', () => {
      cy.visit('/profile');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /invoices to /auth/login when not authenticated', () => {
      cy.visit('/invoices');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /receipts to /auth/login when not authenticated', () => {
      cy.visit('/receipts');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /messages to /auth/login when not authenticated', () => {
      cy.visit('/messages');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /billing to /auth/login when not authenticated', () => {
      cy.visit('/billing');
      cy.url().should('include', '/auth/login');
    });

    it('should redirect /applications to /auth/login when not authenticated', () => {
      cy.visit('/applications');
      cy.url().should('include', '/auth/login');
    });
  });

  describe('Public routes (no auth required)', () => {
    it('should allow access to / (landing) without auth', () => {
      cy.visit('/');
      cy.url().should('not.include', '/auth/login');
      cy.get('.landing').should('exist');
    });

    it('should allow access to /listings without auth', () => {
      // Intercept the listings API so the page does not fail on missing backend
      cy.intercept('GET', '**/api/listings/search*', {
        statusCode: 200,
        body: [],
      }).as('searchListings');

      cy.visit('/listings');
      cy.url().should('include', '/listings');
      cy.url().should('not.include', '/auth/login');
    });

    it('should allow access to /auth/login without auth', () => {
      cy.visit('/auth/login');
      cy.url().should('include', '/auth/login');
      cy.contains('h1', 'Connexion').should('be.visible');
    });

    it('should allow access to /auth/register without auth', () => {
      cy.visit('/auth/register');
      cy.url().should('include', '/auth/register');
      cy.contains('h1', 'Créer un compte').should('be.visible');
    });
  });

  describe('Wildcard route', () => {
    it('should redirect unknown routes to /dashboard (which then redirects to /auth/login)', () => {
      // The catch-all route redirects to /dashboard, and AuthGuard redirects to /auth/login
      cy.visit('/nonexistent-page');
      cy.url().should('include', '/auth/login');
    });
  });

  describe('Authenticated navigation', () => {
    beforeEach(() => {
      cy.login();
    });

    it('should allow access to /dashboard when authenticated', () => {
      // Intercept the dashboard API calls
      cy.intercept('GET', '**/api/dashboard*', {
        statusCode: 200,
        body: {},
      }).as('getDashboard');
      cy.intercept('GET', '**/api/properties*', {
        statusCode: 200,
        body: [],
      }).as('getProperties');
      cy.intercept('GET', '**/api/rentals*', {
        statusCode: 200,
        body: [],
      }).as('getRentals');
      cy.intercept('GET', '**/api/contracts*', {
        statusCode: 200,
        body: [],
      }).as('getContracts');
      // Catch any other common dashboard requests
      cy.intercept('GET', '**/api/**', {
        statusCode: 200,
        body: [],
      });

      cy.visit('/dashboard');
      cy.url().should('include', '/dashboard');
      cy.url().should('not.include', '/auth/login');
    });

    it('should allow access to /contracts when authenticated', () => {
      cy.interceptContractsApi();
      cy.visit('/contracts');
      cy.url().should('include', '/contracts');
      cy.url().should('not.include', '/auth/login');
    });
  });
});
