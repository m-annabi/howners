describe('Contract List', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptContractsApi();
  });

  it('should display the list of contracts', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    cy.contains('Mes Contrats').should('be.visible');
    cy.get('.contract-card').should('have.length', 3);

    // Verify first contract data
    cy.contains('CTR-2024-001').should('be.visible');
    cy.contains('Appartement Paris 11e').should('be.visible');
    cy.contains('Marie Martin').should('be.visible');
    cy.contains('Version 1').should('be.visible');
  });

  it('should display the loading skeleton while loading', () => {
    cy.intercept('GET', '**/api/contracts', (req) => {
      req.reply({
        statusCode: 200,
        body: [],
        delay: 1000,
      });
    }).as('getContractsSlow');

    cy.visit('/contracts');
    cy.get('app-loading-skeleton').should('be.visible');
  });

  it('should display empty state when no contracts', () => {
    cy.intercept('GET', '**/api/contracts', {
      statusCode: 200,
      body: [],
    }).as('getContractsEmpty');

    cy.visit('/contracts');
    cy.wait('@getContractsEmpty');

    cy.get('app-empty-state').should('be.visible');
    cy.contains('Aucun contrat').should('be.visible');
    cy.contains('Nouveau contrat').should('be.visible');
  });

  it('should filter contracts by status', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    cy.get('.contract-list__filter').select('DRAFT');
    cy.get('.contract-card').should('have.length', 1);
    cy.contains('CTR-2024-001').should('be.visible');
    cy.contains('CTR-2024-002').should('not.exist');

    cy.get('.contract-list__filter').select('SENT');
    cy.get('.contract-card').should('have.length', 1);
    cy.contains('CTR-2024-002').should('be.visible');

    cy.get('.contract-list__filter').select('ALL');
    cy.get('.contract-card').should('have.length', 3);
  });

  it('should search by contract number, property or tenant', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    // Search by contract number
    cy.get('.contract-list__search input').type('CTR-2024-001');
    cy.get('.contract-card').should('have.length', 1);
    cy.contains('Appartement Paris 11e').should('be.visible');

    // Clear and search by property
    cy.get('.contract-list__search input').clear().type('Lyon');
    cy.get('.contract-card').should('have.length', 1);
    cy.contains('Pierre Durand').should('be.visible');

    // Clear and search by tenant
    cy.get('.contract-list__search input').clear().type('Sophie');
    cy.get('.contract-card').should('have.length', 1);
    cy.contains('Maison Bordeaux').should('be.visible');
  });

  it('should navigate to contract detail on click', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    cy.contains('.contract-card', 'CTR-2024-001').click();
    cy.url().should('include', '/contracts/draft-001');
  });

  it('should navigate to new contract form', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    cy.contains('Nouveau Contrat').click();
    cy.url().should('include', '/contracts/new');
  });

  it('should delete a DRAFT contract', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    cy.intercept('DELETE', '**/api/contracts/draft-001', {
      statusCode: 204,
    }).as('deleteContract');

    cy.on('window:confirm', () => true);

    cy.contains('.contract-card', 'CTR-2024-001')
      .find('button')
      .contains('Supprimer')
      .click();

    cy.wait('@deleteContract');
  });

  it('should hide delete button for non-DRAFT contracts', () => {
    cy.visit('/contracts');
    cy.wait('@getContracts');

    // SENT contract should not have delete button
    cy.contains('.contract-card', 'CTR-2024-002')
      .find('.contract-card__actions')
      .should('not.exist');

    // ACTIVE contract should not have delete button
    cy.contains('.contract-card', 'CTR-2024-003')
      .find('.contract-card__actions')
      .should('not.exist');

    // DRAFT contract should have delete button
    cy.contains('.contract-card', 'CTR-2024-001')
      .find('.contract-card__actions')
      .should('exist');
  });
});
