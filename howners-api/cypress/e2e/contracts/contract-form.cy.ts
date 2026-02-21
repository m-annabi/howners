describe('Contract Form', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptContractsApi();
  });

  it('should display the form with rental list', () => {
    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    cy.contains('Créer un nouveau contrat').should('be.visible');
    cy.get('#rentalId').should('be.visible');
    cy.get('#rentalId option').should('have.length.greaterThan', 1);
    cy.contains('Appartement Paris 11e - Marie Martin').should('be.visible');
    cy.contains('Studio Lyon 3e - Pierre Durand').should('be.visible');
  });

  it('should display a warning when no rentals available', () => {
    cy.intercept('GET', '**/api/rentals', {
      statusCode: 200,
      body: [],
    }).as('getRentalsEmpty');

    cy.visit('/contracts/new');
    cy.wait('@getRentalsEmpty');

    cy.contains('Aucune location active ou en attente').should('be.visible');
    cy.get('form').should('not.exist');
  });

  it('should enable buttons when a rental is selected', () => {
    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    // Initially buttons should be disabled
    cy.contains('button', 'Génération rapide').should('be.disabled');
    cy.contains('button', 'Personnaliser avant génération').should('be.disabled');

    // Select a rental
    cy.get('#rentalId').select('rental-001');

    // Buttons should now be enabled
    cy.contains('button', 'Génération rapide').should('not.be.disabled');
    cy.contains('button', 'Personnaliser avant génération').should('not.be.disabled');
  });

  it('should create contract via quick generate and redirect', () => {
    const newContract = {
      id: 'new-contract-001',
      contractNumber: 'CTR-2024-004',
      rentalId: 'rental-001',
      rentalPropertyName: 'Appartement Paris 11e',
      tenantFullName: 'Marie Martin',
      status: 'DRAFT',
      currentVersion: 1,
      documentUrl: null,
      createdAt: '2024-07-01T10:00:00Z',
      updatedAt: null,
      sentAt: null,
      signedAt: null,
    };

    cy.intercept('POST', '**/api/contracts', {
      statusCode: 201,
      body: newContract,
    }).as('createContract');

    // Intercept the GET for the newly created contract detail page
    cy.intercept('GET', '**/api/contracts/new-contract-001', {
      statusCode: 200,
      body: newContract,
    }).as('getNewContract');

    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    cy.get('#rentalId').select('rental-001');
    cy.contains('button', 'Génération rapide').click();

    cy.wait('@createContract').its('request.body').should('deep.include', {
      rentalId: 'rental-001',
    });
    cy.url().should('include', '/contracts/new-contract-001');
  });

  it('should redirect to customize page', () => {
    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    cy.get('#rentalId').select('rental-001');
    cy.contains('button', 'Personnaliser avant génération').click();

    cy.url().should('include', '/contracts/customize');
    cy.url().should('include', 'rentalId=rental-001');
  });

  it('should navigate back to contracts list on cancel', () => {
    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    cy.contains('button', 'Annuler').click();
    cy.url().should('match', /\/contracts$/);
  });

  it('should keep buttons disabled when no rental is selected', () => {
    cy.visit('/contracts/new');
    cy.wait('@getRentals');

    cy.contains('button', 'Génération rapide').should('be.disabled');
    cy.contains('button', 'Personnaliser avant génération').should('be.disabled');

    // Select then deselect
    cy.get('#rentalId').select('rental-001');
    cy.contains('button', 'Génération rapide').should('not.be.disabled');

    cy.get('#rentalId').select('');
    cy.contains('button', 'Génération rapide').should('be.disabled');
  });
});
