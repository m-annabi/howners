describe('Contract Amendments', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptContractsApi();
  });

  it('should display the list of amendments', () => {
    cy.visit('/contracts/draft-001/amendments');
    cy.wait('@getAmendments');

    cy.contains('Avenants').should('be.visible');
    cy.get('table tbody tr').should('have.length', 2);

    // First amendment
    cy.contains('Avenant n\u00B01').should('be.visible');
    cy.contains('Augmentation du loyer annuelle').should('be.visible');
    cy.contains('1,200.00 EUR').should('be.visible');
    cy.contains('1,250.00 EUR').should('be.visible');

    // Second amendment
    cy.contains('Avenant n\u00B02').should('be.visible');
    cy.contains('Modification des charges').should('be.visible');
  });

  it('should display empty state when no amendments', () => {
    cy.intercept('GET', '**/api/contracts/*/amendments', {
      statusCode: 200,
      body: [],
    }).as('getAmendmentsEmpty');

    cy.visit('/contracts/draft-001/amendments');
    cy.wait('@getAmendmentsEmpty');

    cy.contains('Aucun avenant pour ce contrat').should('be.visible');
    cy.get('table').should('not.exist');
  });

  it('should navigate to new amendment form', () => {
    cy.visit('/contracts/draft-001/amendments');
    cy.wait('@getAmendments');

    cy.contains('Nouvel avenant').click();
    cy.url().should('include', '/contracts/draft-001/amendments/new');
  });

  it('should create an amendment', () => {
    cy.intercept('GET', '**/api/contracts/draft-001', {
      statusCode: 200,
      body: {
        id: 'draft-001',
        contractNumber: 'CTR-2024-001',
        rentalId: 'rental-001',
        rentalPropertyName: 'Appartement Paris 11e',
        tenantFullName: 'Marie Martin',
        status: 'DRAFT',
        currentVersion: 1,
        documentUrl: null,
        createdAt: '2024-06-01T10:00:00Z',
        updatedAt: null,
        sentAt: null,
        signedAt: null,
      },
    }).as('getContractForAmendment');

    cy.intercept('POST', '**/api/contracts/draft-001/amendments', {
      statusCode: 201,
      body: {
        id: 'amend-003',
        contractId: 'draft-001',
        contractNumber: 'CTR-2024-001',
        amendmentNumber: 3,
        reason: 'Changement de date',
        changes: 'La date de fin est modifiée',
        previousRent: 1200,
        newRent: 1300,
        effectiveDate: '2025-06-01',
        status: 'DRAFT',
        createdByName: 'Jean Dupont',
        signedAt: null,
        documentId: null,
        createdAt: '2024-12-01T10:00:00Z',
      },
    }).as('createAmendment');

    cy.visit('/contracts/draft-001/amendments/new');
    cy.wait('@getContractForAmendment');

    cy.contains('Contrat CTR-2024-001').should('be.visible');

    // Fill form
    cy.get('textarea[name="reason"]').type('Changement de date');
    cy.get('textarea[name="changes"]').type('La date de fin est modifiée');
    cy.get('input[name="previousRent"]').type('1200');
    cy.get('input[name="newRent"]').type('1300');
    cy.get('input[name="effectiveDate"]').type('2025-06-01');

    // Submit
    cy.contains('button', "Créer l'avenant").click();
    cy.wait('@createAmendment').its('request.body').should('deep.include', {
      reason: 'Changement de date',
      effectiveDate: '2025-06-01',
    });

    cy.url().should('include', '/contracts/draft-001/amendments');
  });
});
