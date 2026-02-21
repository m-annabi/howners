describe('Template Variable Helper', () => {
  beforeEach(() => {
    cy.login();
    cy.interceptTemplatesApi();
  });

  it('should display all variables on the form page', () => {
    cy.visit('/templates/new');
    cy.wait('@getVariables');

    cy.get('app-variable-helper').should('exist');
    cy.contains('Variables Disponibles').should('exist');

    // Check variables exist (some may need scrolling in the sticky panel)
    cy.get('.variable-item').should('have.length', 9);
    cy.contains('.variable-key', 'owner_name').should('exist');
    cy.contains('.variable-key', 'tenant_name').should('exist');
    cy.contains('.variable-key', 'property_address').should('exist');
    cy.contains('.variable-key', 'rental_start_date').should('exist');
    cy.contains('.variable-key', 'current_date').should('exist');
  });

  it('should filter variables by category', () => {
    cy.visit('/templates/new');
    cy.wait('@getVariables');

    // Filter to owner category
    cy.get('app-variable-helper select').select('owner');
    cy.get('.variable-item').should('have.length', 2);
    cy.contains('.variable-key', 'owner_name').should('exist');
    cy.contains('.variable-key', 'owner_email').should('exist');
    cy.contains('.variable-key', 'tenant_name').should('not.exist');

    // Filter to tenant
    cy.get('app-variable-helper select').select('tenant');
    cy.get('.variable-item').should('have.length', 2);
    cy.contains('.variable-key', 'tenant_name').should('exist');

    // Back to all
    cy.get('app-variable-helper select').select('all');
    cy.get('.variable-item').should('have.length', 9);
  });

  it('should search variables', () => {
    cy.visit('/templates/new');
    cy.wait('@getVariables');

    cy.get('app-variable-helper input[type="text"]').type('email');
    cy.get('.variable-item').should('have.length', 2);
    cy.contains('.variable-key', 'owner_email').should('exist');
    cy.contains('.variable-key', 'tenant_email').should('exist');
  });

  it('should display variable examples', () => {
    cy.visit('/templates/new');
    cy.wait('@getVariables');

    // Filter to reduce items so they're visible without scrolling
    cy.get('app-variable-helper select').select('owner');
    cy.get('.variable-item').first().scrollIntoView();
    cy.contains('.variable-example', 'Jean Dupont').should('exist');

    cy.get('app-variable-helper select').select('tenant');
    cy.get('.variable-item').first().scrollIntoView();
    cy.contains('.variable-example', 'marie.martin@example.com').should('exist');
  });

  it('should display empty state when no variables match search', () => {
    cy.visit('/templates/new');
    cy.wait('@getVariables');

    cy.get('app-variable-helper input[type="text"]').type('zzzzz');
    cy.contains('Aucune variable trouvée').should('be.visible');
  });
});
