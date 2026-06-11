describe('Mobile Responsiveness', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  describe('Landing page at mobile viewport (375x667)', () => {
    beforeEach(() => {
      cy.viewport(375, 667);
      cy.visit('/');
    });

    it('should render the landing page without horizontal overflow', () => {
      // The body should not be wider than the viewport
      cy.document().then((doc) => {
        const body = doc.body;
        const html = doc.documentElement;
        expect(body.scrollWidth).to.be.at.most(html.clientWidth + 1);
      });
    });

    it('should render the hero section', () => {
      cy.get('.hero').should('be.visible');
      cy.get('.hero__copy h1').should('be.visible');
    });

    it('should render the pricing cards', () => {
      cy.get('.plan-card').should('have.length', 3);
      // All three plan names should be present in the DOM
      cy.contains('.plan-card h3', 'Gratuit').should('exist');
      cy.contains('.plan-card h3', 'Pro').should('exist');
      cy.contains('.plan-card h3', 'Premium').should('exist');
    });

    it('should render FAQ items that are expandable', () => {
      cy.get('.faq-item').should('have.length', 4);
      cy.get('.faq-item').first().find('summary').click();
      cy.get('.faq-item').first().should('have.attr', 'open');
    });

    it('should render the footer', () => {
      cy.get('.landing-footer').should('exist');
      cy.get('.landing-footer__brand').should('contain.text', 'Howners');
    });

    it('should render social proof stats', () => {
      cy.get('.social-proof__stats').should('exist');
      cy.get('.stat').should('have.length', 4);
    });

    it('should render feature cards', () => {
      cy.get('.feature-card').should('have.length', 4);
    });
  });

  describe('Listings page at tablet viewport (768x1024)', () => {
    const sampleListings = [
      {
        id: 'listing-001',
        title: 'Appartement T2 Paris',
        description: 'Bel appartement au coeur de Paris.',
        propertyCity: 'Paris',
        propertyPostalCode: '75011',
        pricePerMonth: 950,
        pricePerNight: null,
        currency: 'EUR',
        photos: [],
        status: 'ACTIVE',
      },
      {
        id: 'listing-002',
        title: 'Studio Marseille',
        description: 'Studio meublé proche mer.',
        propertyCity: 'Marseille',
        propertyPostalCode: '13001',
        pricePerMonth: 450,
        pricePerNight: null,
        currency: 'EUR',
        photos: [],
        status: 'ACTIVE',
      },
    ];

    beforeEach(() => {
      cy.viewport(768, 1024);
      cy.intercept('GET', '**/api/listings/search*', {
        statusCode: 200,
        body: sampleListings,
      }).as('searchListings');
      cy.visit('/listings');
      cy.wait('@searchListings');
    });

    it('should render the listings page without horizontal overflow', () => {
      cy.document().then((doc) => {
        const body = doc.body;
        const html = doc.documentElement;
        expect(body.scrollWidth).to.be.at.most(html.clientWidth + 1);
      });
    });

    it('should display the search bar', () => {
      cy.get('.listing-search__query input[type="text"]').should('be.visible');
    });

    it('should display the filter bar', () => {
      cy.get('.filter-bar').should('be.visible');
    });

    it('should display listing cards', () => {
      cy.get('.card').should('have.length', 2);
    });

    it('should display listing titles and cities', () => {
      cy.contains('.card-title', 'Appartement T2 Paris').should('be.visible');
      cy.contains('.card-title', 'Studio Marseille').should('be.visible');
    });
  });

  describe('Login page at mobile viewport (375x667)', () => {
    beforeEach(() => {
      cy.viewport(375, 667);
      cy.visit('/auth/login');
    });

    it('should render the login form without horizontal overflow', () => {
      cy.document().then((doc) => {
        const body = doc.body;
        const html = doc.documentElement;
        expect(body.scrollWidth).to.be.at.most(html.clientWidth + 1);
      });
    });

    it('should render all form elements', () => {
      cy.contains('h1', 'Connexion').should('be.visible');
      cy.get('input#email').should('be.visible');
      cy.get('input#password').should('be.visible');
      cy.get('button[type="submit"]').should('be.visible');
    });
  });

  describe('Register page at mobile viewport (375x667)', () => {
    beforeEach(() => {
      cy.viewport(375, 667);
      cy.visit('/auth/register');
    });

    it('should render the registration form without horizontal overflow', () => {
      cy.document().then((doc) => {
        const body = doc.body;
        const html = doc.documentElement;
        expect(body.scrollWidth).to.be.at.most(html.clientWidth + 1);
      });
    });

    it('should render all form elements', () => {
      cy.contains('h1', 'Créer un compte').should('be.visible');
      cy.get('input#firstName').should('be.visible');
      cy.get('input#lastName').should('be.visible');
      cy.get('input#email').should('be.visible');
      cy.get('input#password').should('be.visible');
      cy.get('select#role').should('be.visible');
      cy.get('button[type="submit"]').should('be.visible');
    });
  });
});
