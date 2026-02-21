describe('Contract Detail', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptContractsApi();
  });

  it('should display contract details', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    // General info
    cy.contains('Contrat CTR-2024-001').should('be.visible');
    cy.contains('Appartement Paris 11e').should('be.visible');
    cy.contains('Marie Martin').should('be.visible');
    cy.contains('1').should('be.visible'); // version

    // Dates
    cy.contains('01/06/2024').should('be.visible'); // createdAt
  });

  it('should display the correct status badge', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.get('.badge').should('contain', 'Brouillon');

    // Visit SENT contract
    cy.visit('/contracts/sent-001');
    cy.wait('@getContractSent');

    cy.get('.badge').should('contain', 'Envoyé');
  });

  it('should navigate back to the list', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.contains('Retour à la liste').click();
    cy.url().should('match', /\/contracts$/);
  });

  it('should download PDF via window.open', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.window().then((win) => {
      cy.stub(win, 'open').as('windowOpen');
    });

    cy.contains('button', 'Télécharger le PDF').click();
    cy.get('@windowOpen').should(
      'be.calledWith',
      'https://storage.example.com/contracts/ctr-001.pdf',
      '_blank'
    );
  });

  it('should load and display version history', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.contains('button', 'Voir les versions').click();
    cy.wait('@getVersions');

    cy.contains('h5', 'Historique des versions').should('be.visible');
    cy.get('.versions table tbody tr').should('have.length', 2);
    cy.get('.versions table').should('contain', 'Jean Dupont');
    cy.get('.versions table').should('contain', 'a1b2c3d4e5f6a7b8');
  });

  it('should send a DRAFT contract for signature', () => {
    cy.intercept('PUT', '**/api/contracts/draft-001', {
      statusCode: 200,
      body: {
        id: 'draft-001',
        contractNumber: 'CTR-2024-001',
        rentalId: 'rental-001',
        rentalPropertyName: 'Appartement Paris 11e',
        tenantFullName: 'Marie Martin',
        status: 'SENT',
        currentVersion: 1,
        documentUrl: 'https://storage.example.com/contracts/ctr-001.pdf',
        createdAt: '2024-06-01T10:00:00Z',
        updatedAt: '2024-06-10T09:00:00Z',
        sentAt: '2024-06-10T09:00:00Z',
        signedAt: null,
      },
    }).as('updateContract');

    cy.visit('/contracts/draft-001');

    // Wait for page to load and button to be visible
    cy.contains('button', 'Envoyer pour signature').should('be.visible');

    cy.window().then((win) => {
      // Stub confirm on the actual loaded window
      const stub = cy.stub(win, 'confirm').returns(true);
      cy.contains('button', 'Envoyer pour signature').click();
      cy.wait('@updateContract');
      cy.get('.badge').should('contain', 'Envoyé');
    });
  });

  it('should delete a DRAFT contract', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.intercept('DELETE', '**/api/contracts/draft-001', {
      statusCode: 204,
    }).as('deleteContract');

    cy.on('window:confirm', () => true);

    cy.contains('button', 'Supprimer').click();
    cy.wait('@deleteContract');
    cy.url().should('match', /\/contracts$/);
  });

  it('should hide delete button for non-DRAFT contracts', () => {
    cy.visit('/contracts/sent-001');
    cy.wait('@getContractSent');

    cy.contains('button', 'Supprimer').should('not.exist');
    cy.contains('button', 'Envoyer pour signature').should('not.exist');
  });

  it('should navigate to amendments', () => {
    cy.visit('/contracts/draft-001');
    cy.wait('@getContractDraft');

    cy.contains('Avenants').click();
    cy.url().should('include', '/contracts/draft-001/amendments');
  });

  it('should display existing signatures', () => {
    cy.visit('/contracts/sent-001');
    cy.wait('@getContractSent');
    cy.wait('@getSignatures');

    cy.contains('Signatures').should('be.visible');
    cy.contains('Pierre Durand').should('be.visible');
  });
});
