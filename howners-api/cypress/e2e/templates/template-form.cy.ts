describe('Template Form', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptTemplatesApi();
  });

  describe('Create mode', () => {
    it('should display the creation form', () => {
      cy.visit('/templates/new');

      cy.contains('Nouveau Template').should('be.visible');
      cy.get('#name').should('be.visible');
      cy.get('#description').should('be.visible');
      cy.get('#rentalType').should('be.visible');
      cy.get('quill-editor').should('be.visible');
    });

    it('should display the variable helper panel', () => {
      cy.visit('/templates/new');
      cy.wait('@getVariables');

      cy.contains('Variables Disponibles').should('be.visible');
      cy.contains('Nom du propriétaire').should('be.visible');
      cy.contains('Nom du locataire').should('be.visible');
    });

    it('should have submit button disabled when form is empty', () => {
      cy.visit('/templates/new');

      cy.contains('button', 'Créer le template').should('be.disabled');
    });

    it('should create a template and redirect', () => {
      cy.intercept('POST', '**/api/contract-templates', {
        statusCode: 201,
        body: {
          id: 'tpl-new',
          name: 'Mon nouveau template',
          description: 'Un template test',
          rentalType: 'LONG_TERM',
          content: '<p>Contenu du template</p>',
          isDefault: false,
          isActive: true,
          createdById: 'user-owner-001',
          createdByName: 'Jean Dupont',
          createdAt: '2024-07-01T10:00:00Z',
          updatedAt: null,
        },
      }).as('createTemplate');

      cy.visit('/templates/new');

      cy.get('#name').type('Mon nouveau template');
      cy.get('#description').type('Un template test');
      cy.get('#rentalType').select('LONG_TERM');
      cy.get('quill-editor .ql-editor').type('Contenu du template');

      cy.contains('button', 'Créer le template').should('not.be.disabled');
      cy.contains('button', 'Créer le template').click();

      cy.wait('@createTemplate').its('request.body').should('deep.include', {
        name: 'Mon nouveau template',
        description: 'Un template test',
        rentalType: 'LONG_TERM',
      });

      cy.url().should('match', /\/templates$/);
    });

    it('should navigate back on cancel', () => {
      cy.visit('/templates/new');

      cy.contains('button', 'Annuler').click();
      cy.url().should('match', /\/templates$/);
    });
  });

  describe('Edit mode', () => {
    it('should load and display the template data', () => {
      cy.visit('/templates/tpl-002/edit');
      cy.wait('@getTemplateDetail');

      cy.contains('Modifier le Template').should('be.visible');
      cy.get('#name').should('have.value', 'Location saisonnière');
      cy.get('#description').should(
        'have.value',
        'Template pour les locations courte durée / saisonnières'
      );
      cy.get('#rentalType').should('have.value', 'SHORT_TERM');
    });

    it('should show error for default templates', () => {
      cy.visit('/templates/tpl-001/edit');
      cy.wait('@getTemplateDefault');

      cy.contains('Les templates par défaut ne peuvent pas être modifiés').should(
        'be.visible'
      );
      cy.get('form').should('not.exist');
    });

    it('should update a template and redirect', () => {
      cy.intercept('PUT', '**/api/contract-templates/tpl-002', {
        statusCode: 200,
        body: {
          id: 'tpl-002',
          name: 'Location saisonnière modifié',
          description: 'Description modifiée',
          rentalType: 'SHORT_TERM',
          content: '<h1>Contenu modifié</h1>',
          isDefault: false,
          isActive: true,
          createdById: 'user-owner-001',
          createdByName: 'Jean Dupont',
          createdAt: '2024-02-01T10:00:00Z',
          updatedAt: '2024-07-01T10:00:00Z',
        },
      }).as('updateTemplate');

      cy.visit('/templates/tpl-002/edit');
      cy.wait('@getTemplateDetail');

      cy.get('#name').clear().type('Location saisonnière modifié');
      cy.contains('button', 'Mettre à jour').click();

      cy.wait('@updateTemplate');
      cy.url().should('match', /\/templates$/);
    });
  });
});
