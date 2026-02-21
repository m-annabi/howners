describe('Template List', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptTemplatesApi();
  });

  it('should display the list of templates', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.contains('Mes Templates de Contrat').should('be.visible');
    cy.get('table tbody tr').should('have.length', 3);

    cy.contains('Bail longue durée standard').should('be.visible');
    cy.contains('Location saisonnière').should('be.visible');
    cy.contains('Bail meublé').should('be.visible');
  });

  it('should display the default badge on default templates', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.contains('tr', 'Bail longue durée standard')
      .find('.badge.bg-info')
      .should('contain', 'Par défaut');

    cy.contains('tr', 'Location saisonnière')
      .find('.badge.bg-info')
      .should('not.exist');
  });

  it('should display rental type badges', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.contains('tr', 'Bail longue durée standard')
      .should('contain', 'Location longue durée');

    cy.contains('tr', 'Location saisonnière')
      .should('contain', 'Location courte durée');
  });

  it('should display loading state', () => {
    cy.intercept('GET', '**/api/contract-templates', (req) => {
      req.reply({
        statusCode: 200,
        body: [],
        delay: 1000,
      });
    }).as('getTemplatesSlow');

    cy.visit('/templates');
    cy.contains('Chargement des templates...').should('be.visible');
  });

  it('should display empty state when no templates', () => {
    cy.intercept('GET', '**/api/contract-templates', {
      statusCode: 200,
      body: [],
    }).as('getTemplatesEmpty');

    cy.visit('/templates');
    cy.wait('@getTemplatesEmpty');

    cy.contains('Aucun template trouvé').should('be.visible');
    cy.get('table').should('not.exist');
  });

  it('should filter templates by rental type', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.get('.form-select').select('LONG_TERM');
    cy.get('table tbody tr').should('have.length', 2);
    cy.contains('Bail longue durée standard').should('be.visible');
    cy.contains('Bail meublé').should('be.visible');
    cy.contains('Location saisonnière').should('not.exist');

    cy.get('.form-select').select('SHORT_TERM');
    cy.get('table tbody tr').should('have.length', 1);
    cy.contains('Location saisonnière').should('be.visible');

    cy.get('.form-select').select('ALL');
    cy.get('table tbody tr').should('have.length', 3);
  });

  it('should search templates by name or description', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    // Search by name
    cy.get('input[type="text"]').type('meublé');
    cy.get('table tbody tr').should('have.length', 1);
    cy.contains('Bail meublé').should('be.visible');

    // Search by description
    cy.get('input[type="text"]').clear().type('saisonnières');
    cy.get('table tbody tr').should('have.length', 1);
    cy.contains('Location saisonnière').should('be.visible');

    // No results
    cy.get('input[type="text"]').clear().type('inexistant');
    cy.contains('Aucun template trouvé').should('be.visible');
  });

  it('should navigate to new template form', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.contains('Nouveau Template').click();
    cy.url().should('include', '/templates/new');
  });

  it('should navigate to edit for non-default templates', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.contains('tr', 'Location saisonnière')
      .find('button[title="Modifier"]')
      .click();

    cy.url().should('include', '/templates/tpl-002/edit');
  });

  it('should hide edit and delete buttons for default templates', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    // Default template should not have edit or delete buttons
    cy.contains('tr', 'Bail longue durée standard').within(() => {
      cy.get('button[title="Modifier"]').should('not.exist');
      cy.get('button[title="Supprimer"]').should('not.exist');
      // But duplicate should be available
      cy.get('button[title="Dupliquer"]').should('exist');
    });

    // Non-default templates should have all buttons
    cy.contains('tr', 'Location saisonnière').within(() => {
      cy.get('button[title="Modifier"]').should('exist');
      cy.get('button[title="Supprimer"]').should('exist');
      cy.get('button[title="Dupliquer"]').should('exist');
    });
  });

  it('should duplicate a template', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.intercept('POST', '**/api/contract-templates/tpl-002/duplicate*', {
      statusCode: 201,
      body: {
        id: 'tpl-004',
        name: 'Copie de Location saisonnière',
        description: 'Template pour les locations courte durée / saisonnières',
        rentalType: 'SHORT_TERM',
        content: '<h1>Contrat de location saisonnière</h1>',
        isDefault: false,
        isActive: true,
        createdById: 'user-owner-001',
        createdByName: 'Jean Dupont',
        createdAt: '2024-07-01T10:00:00Z',
        updatedAt: null,
      },
    }).as('duplicateTemplate');

    cy.window().then((win) => {
      cy.stub(win, 'prompt').returns('Copie de Location saisonnière');
    });

    cy.contains('tr', 'Location saisonnière')
      .find('button[title="Dupliquer"]')
      .click();

    cy.wait('@duplicateTemplate');
  });

  it('should delete a non-default template', () => {
    cy.visit('/templates');
    cy.wait('@getTemplatesList');

    cy.intercept('DELETE', '**/api/contract-templates/tpl-002', {
      statusCode: 204,
    }).as('deleteTemplate');

    cy.on('window:confirm', () => true);

    cy.contains('tr', 'Location saisonnière')
      .find('button[title="Supprimer"]')
      .click();

    cy.wait('@deleteTemplate');
  });
});
