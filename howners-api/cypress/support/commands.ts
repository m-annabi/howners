declare namespace Cypress {
  interface Chainable {
    login(): Chainable<void>;
    interceptContractsApi(): Chainable<void>;
    interceptTemplatesApi(): Chainable<void>;
  }
}

Cypress.Commands.add('login', () => {
  cy.fixture('user.json').then((user) => {
    window.localStorage.setItem('access_token', 'fake-jwt-token-for-testing');

    cy.intercept('GET', '**/api/auth/me', {
      statusCode: 200,
      body: user,
    }).as('getMe');

    // Intercept global app calls that happen on every page load
    cy.intercept('GET', '**/api/messages/unread-count', {
      statusCode: 200,
      body: { count: 0 },
    }).as('getUnreadCount');
  });
});

Cypress.Commands.add('interceptContractsApi', () => {
  cy.fixture('contracts.json').then((contracts) => {
    cy.intercept('GET', '**/api/contracts', {
      statusCode: 200,
      body: contracts,
    }).as('getContracts');
  });

  cy.fixture('contract-draft.json').then((contract) => {
    cy.intercept('GET', '**/api/contracts/draft-001', {
      statusCode: 200,
      body: contract,
    }).as('getContractDraft');
  });

  cy.fixture('contract-sent.json').then((contract) => {
    cy.intercept('GET', '**/api/contracts/sent-001', {
      statusCode: 200,
      body: contract,
    }).as('getContractSent');
  });

  cy.fixture('rentals.json').then((rentals) => {
    cy.intercept('GET', '**/api/rentals', {
      statusCode: 200,
      body: rentals,
    }).as('getRentals');
  });

  cy.fixture('templates.json').then((templates) => {
    cy.intercept('GET', '**/api/contract-templates*', {
      statusCode: 200,
      body: templates,
    }).as('getTemplates');
  });

  cy.fixture('versions.json').then((versions) => {
    cy.intercept('GET', '**/api/contracts/*/versions', {
      statusCode: 200,
      body: versions,
    }).as('getVersions');
  });

  cy.fixture('signatures.json').then((signatures) => {
    cy.intercept('GET', '**/api/signatures/contract/*', {
      statusCode: 200,
      body: signatures,
    }).as('getSignatures');
  });

  cy.fixture('amendments.json').then((amendments) => {
    cy.intercept('GET', '**/api/contracts/*/amendments', {
      statusCode: 200,
      body: amendments,
    }).as('getAmendments');
  });

  // Intercept e-signature status - return empty to avoid ErrorInterceptor toast
  cy.intercept('GET', '**/api/contracts/*/esignature/status', {
    statusCode: 200,
    body: null,
  }).as('getEsignatureStatus');
});

Cypress.Commands.add('interceptTemplatesApi', () => {
  cy.fixture('templates-list.json').then((templates) => {
    cy.intercept('GET', '**/api/contract-templates', {
      statusCode: 200,
      body: templates,
    }).as('getTemplatesList');
  });

  // Match with query params too
  cy.fixture('templates-list.json').then((templates) => {
    cy.intercept('GET', '**/api/contract-templates?*', {
      statusCode: 200,
      body: templates,
    }).as('getTemplatesFiltered');
  });

  cy.fixture('template-detail.json').then((template) => {
    cy.intercept('GET', '**/api/contract-templates/tpl-002', {
      statusCode: 200,
      body: template,
    }).as('getTemplateDetail');
  });

  cy.fixture('template-default.json').then((template) => {
    cy.intercept('GET', '**/api/contract-templates/tpl-001', {
      statusCode: 200,
      body: template,
    }).as('getTemplateDefault');
  });

  cy.fixture('template-variables.json').then((variables) => {
    cy.intercept('GET', '**/api/contract-templates/variables', {
      statusCode: 200,
      body: variables,
    }).as('getVariables');
  });
});
