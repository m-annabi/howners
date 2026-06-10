describe('Public Listings Page', () => {
  const sampleListings = [
    {
      id: 'listing-001',
      title: 'Appartement T3 lumineux',
      description: 'Bel appartement T3 avec balcon, proche transports.',
      propertyCity: 'Lyon',
      propertyPostalCode: '69003',
      pricePerMonth: 850,
      pricePerNight: null,
      currency: 'EUR',
      photos: [],
      status: 'ACTIVE',
      availableFrom: '2026-07-01',
      propertyType: 'APARTMENT',
      surface: 65,
      bedrooms: 2,
      furnished: false,
    },
    {
      id: 'listing-002',
      title: 'Studio meublé centre-ville',
      description: 'Studio entièrement meublé au coeur de Bordeaux.',
      propertyCity: 'Bordeaux',
      propertyPostalCode: '33000',
      pricePerMonth: 550,
      pricePerNight: null,
      currency: 'EUR',
      photos: [{ fileUrl: 'https://example.com/photo.jpg' }],
      status: 'ACTIVE',
      availableFrom: '2026-06-15',
      propertyType: 'STUDIO',
      surface: 25,
      bedrooms: 0,
      furnished: true,
    },
    {
      id: 'listing-003',
      title: 'Maison avec jardin',
      description: 'Grande maison familiale avec jardin privatif.',
      propertyCity: 'Nantes',
      propertyPostalCode: '44000',
      pricePerMonth: 1200,
      pricePerNight: null,
      currency: 'EUR',
      photos: [],
      status: 'ACTIVE',
      availableFrom: '2026-08-01',
      propertyType: 'HOUSE',
      surface: 120,
      bedrooms: 4,
      furnished: false,
    },
  ];

  function interceptListingsApi(listings = sampleListings): void {
    cy.intercept('GET', '**/api/listings/search*', {
      statusCode: 200,
      body: listings,
    }).as('searchListings');
  }

  beforeEach(() => {
    cy.clearLocalStorage();
  });

  describe('Page load', () => {
    it('should load the listings page without authentication', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');
      cy.url().should('include', '/listings');
      // Should NOT be redirected to login
      cy.url().should('not.include', '/auth/login');
    });

    it('should display the page heading', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');
      cy.contains('h2', 'Rechercher des annonces').should('be.visible');
      cy.contains('Filtrez par localisation').should('be.visible');
    });
  });

  describe('Search input', () => {
    it('should display the search input with placeholder', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.get('.listing-search__query input[type="text"]')
        .should('be.visible')
        .and('have.attr', 'placeholder')
        .and('contain', 'Mot-clé');
    });

    it('should display the search button', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.get('.listing-search__query').contains('button', 'Rechercher').should('be.visible');
    });

    it('should trigger search on button click', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      // Type a query and search again
      interceptListingsApi([sampleListings[0]]);
      cy.get('.listing-search__query input[type="text"]').type('Lyon');
      cy.get('.listing-search__query').contains('button', 'Rechercher').click();
      cy.wait('@searchListings');
    });
  });

  describe('Filter controls', () => {
    beforeEach(() => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');
    });

    it('should display the primary filter bar', () => {
      cy.get('.filter-bar').should('be.visible');
    });

    it('should display a city / location input', () => {
      cy.get('.filter-bar__field--location input[type="text"]')
        .should('be.visible')
        .and('have.attr', 'placeholder', 'Ville ou code postal');
    });

    it('should display price max input', () => {
      cy.contains('.filter-bar__label', 'Prix max').should('be.visible');
      cy.get('.filter-bar').find('input[type="number"]').should('exist');
    });

    it('should display surface min input', () => {
      cy.contains('.filter-bar__label', 'Surface min').should('be.visible');
    });

    it('should display property type selector', () => {
      cy.contains('.filter-bar__label', 'Type').should('be.visible');
      cy.get('.filter-bar select').first().should('contain', 'Tous');
    });

    it('should display sort selector', () => {
      cy.contains('.filter-bar__label', 'Tri').should('be.visible');
    });

    it('should display the "Plus" (more filters) toggle button', () => {
      cy.get('.filter-bar__actions').contains('button', 'Plus').should('be.visible');
    });

    it('should toggle secondary filters on "Plus" click', () => {
      // Secondary filters should not be visible initially
      cy.get('.filter-more').should('not.exist');

      // Click "Plus" to reveal
      cy.get('.filter-bar__actions').contains('button', 'Plus').click();
      cy.get('.filter-more').should('be.visible');

      // Should show department filter
      cy.get('.filter-more').contains('label', 'Département').should('be.visible');

      // Click again to hide
      cy.get('.filter-bar__actions').contains('button', 'Plus').click();
      cy.get('.filter-more').should('not.exist');
    });

    it('should display geolocate button "Près de moi"', () => {
      cy.get('.filter-bar__locate').should('be.visible');
      cy.get('.filter-bar__locate-label').should('contain.text', 'Près de moi');
    });
  });

  describe('Listing cards', () => {
    it('should render listing cards when results exist', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.get('.card').should('have.length', 3);
    });

    it('should display listing title, city, and price on each card', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      // First listing
      cy.contains('.card-title', 'Appartement T3 lumineux').should('be.visible');
      cy.contains('.card', 'Appartement T3 lumineux').within(() => {
        cy.contains('Lyon').should('be.visible');
        cy.contains('69003').should('be.visible');
        cy.get('.badge').should('contain.text', 'EUR/mois');
      });
    });

    it('should display "Voir l\'annonce" link on each card', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.get('.card-footer a').should('have.length', 3);
      cy.get('.card-footer a').first().should('contain.text', "Voir l'annonce");
    });

    it('should display results count', () => {
      interceptListingsApi();
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.get('.listing-search__count').should('contain.text', '3 annonces');
    });
  });

  describe('Empty state', () => {
    it('should display empty state when no listings match', () => {
      interceptListingsApi([]);
      cy.visit('/listings');
      cy.wait('@searchListings');

      cy.contains('Aucune annonce trouvée').should('be.visible');
      cy.get('.bi-house-slash').should('be.visible');
    });
  });

  describe('Loading state', () => {
    it('should display a spinner while loading', () => {
      cy.intercept('GET', '**/api/listings/search*', (req) => {
        req.reply({
          statusCode: 200,
          body: sampleListings,
          delay: 1000,
        });
      }).as('searchListingsSlow');

      cy.visit('/listings');
      cy.get('.spinner-border').should('be.visible');
    });
  });
});
