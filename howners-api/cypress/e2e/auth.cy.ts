describe('Authentication Flows', () => {
  beforeEach(() => {
    // Ensure no token is stored so we hit the public pages
    cy.clearLocalStorage();
  });

  describe('Login page', () => {
    beforeEach(() => {
      cy.visit('/auth/login');
    });

    it('should render the login form with all expected elements', () => {
      cy.contains('h1', 'Connexion').should('be.visible');
      cy.contains('Bienvenue sur Howners').should('be.visible');

      // Form fields
      cy.get('input#email[formControlName="email"]').should('exist');
      cy.get('input#password[formControlName="password"]').should('exist');

      // Labels
      cy.get('label[for="email"]').should('contain.text', 'Adresse email');
      cy.get('label[for="password"]').should('contain.text', 'Mot de passe');

      // Submit button
      cy.get('button[type="submit"]').should('contain.text', 'Se connecter');

      // Link to register
      cy.contains('a', 'Créer un compte')
        .should('have.attr', 'href', '/auth/register');
    });

    it('should show validation error when submitting empty fields', () => {
      // Touch the fields by focusing and blurring
      cy.get('input#email').focus().blur();
      cy.get('input#password').focus().blur();

      // Submit button should be disabled when the form is invalid
      cy.get('button[type="submit"]').should('be.disabled');

      // Validation messages should appear
      cy.contains('Veuillez entrer une adresse email valide').should('be.visible');
      cy.contains('Mot de passe requis').should('be.visible');
    });

    it('should show error message on invalid credentials', () => {
      cy.intercept('POST', '**/api/auth/login', {
        statusCode: 401,
        body: { message: 'Email ou mot de passe incorrect' },
      }).as('loginFail');

      cy.get('input#email').type('wrong@example.com');
      cy.get('input#password').type('WrongPass1');
      cy.get('button[type="submit"]').click();

      cy.wait('@loginFail');
      cy.get('.hw-alert--danger').should('be.visible');
      cy.get('.hw-alert--danger').should('contain.text', 'Email ou mot de passe incorrect');
    });

    it('should keep submit button disabled with an invalid email format', () => {
      cy.get('input#email').type('not-an-email');
      cy.get('input#password').type('ValidPass1');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('should keep submit button disabled with a password shorter than 8 characters', () => {
      cy.get('input#email').type('user@example.com');
      cy.get('input#password').type('Abc1');
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('should toggle password visibility', () => {
      cy.get('input#password').should('have.attr', 'type', 'password');
      cy.get('.password-toggle').click();
      cy.get('input#password').should('have.attr', 'type', 'text');
      cy.get('.password-toggle').click();
      cy.get('input#password').should('have.attr', 'type', 'password');
    });

    it('should navigate to register page via link', () => {
      cy.contains('a', 'Créer un compte').click();
      cy.url().should('include', '/auth/register');
    });
  });

  describe('Register page', () => {
    beforeEach(() => {
      cy.visit('/auth/register');
    });

    it('should render the registration form with all expected elements', () => {
      cy.contains('h1', 'Créer un compte').should('be.visible');
      cy.contains('Rejoignez Howners').should('be.visible');

      // Name fields
      cy.get('input#firstName[formControlName="firstName"]').should('exist');
      cy.get('input#lastName[formControlName="lastName"]').should('exist');

      // Other fields
      cy.get('input#email[formControlName="email"]').should('exist');
      cy.get('input#password[formControlName="password"]').should('exist');
      cy.get('input#phone[formControlName="phone"]').should('exist');
      cy.get('select#role[formControlName="role"]').should('exist');

      // Role options
      cy.get('select#role').should('contain', 'Propriétaire');
      cy.get('select#role').should('contain', 'Locataire');

      // Submit button
      cy.get('button[type="submit"]').should('contain.text', 'Créer mon compte');

      // Link to login
      cy.contains('a', 'Se connecter')
        .should('have.attr', 'href', '/auth/login');
    });

    it('should keep submit button disabled when required fields are empty', () => {
      // Form starts empty so submit should be disabled
      cy.get('button[type="submit"]').should('be.disabled');
    });

    it('should show validation error for a weak password (too short)', () => {
      cy.get('input#password').type('Ab1');
      cy.get('input#password').blur();

      cy.contains('Le mot de passe doit contenir au moins 8 caractères').should('be.visible');
    });

    it('should show validation error for a password missing uppercase/lowercase/digit', () => {
      // 8+ chars but only lowercase — missing uppercase and digit
      cy.get('input#password').type('abcdefgh');
      cy.get('input#password').blur();

      cy.contains('Le mot de passe doit contenir une majuscule, une minuscule et un chiffre').should('be.visible');
    });

    it('should show error on failed registration', () => {
      cy.intercept('POST', '**/api/auth/register', {
        statusCode: 409,
        body: { message: 'Un compte avec cet email existe déjà' },
      }).as('registerFail');

      cy.get('input#firstName').type('Jean');
      cy.get('input#lastName').type('Dupont');
      cy.get('input#email').type('existing@example.com');
      cy.get('input#password').type('StrongPass1');
      cy.get('button[type="submit"]').click();

      cy.wait('@registerFail');
      cy.get('.hw-alert--danger').should('be.visible');
      cy.get('.hw-alert--danger').should('contain.text', 'Un compte avec cet email existe déjà');
    });

    it('should navigate to login page via link', () => {
      cy.contains('a', 'Se connecter').click();
      cy.url().should('include', '/auth/login');
    });

    it('should toggle password visibility', () => {
      cy.get('input#password').should('have.attr', 'type', 'password');
      cy.get('.password-toggle').click();
      cy.get('input#password').should('have.attr', 'type', 'text');
    });
  });

  describe('Landing page auth buttons', () => {
    it('should display "Connexion" and "Créer un compte" buttons on the landing page', () => {
      cy.visit('/');
      cy.get('.landing-nav__cta').within(() => {
        cy.contains('button', 'Connexion').should('be.visible');
        cy.contains('button', 'Créer un compte').should('be.visible');
      });
    });

    it('should navigate to login when clicking "Connexion"', () => {
      cy.visit('/');
      cy.get('.landing-nav__cta').contains('button', 'Connexion').click();
      cy.url().should('include', '/auth/login');
    });

    it('should navigate to register when clicking "Créer un compte"', () => {
      cy.visit('/');
      cy.get('.landing-nav__cta').contains('button', 'Créer un compte').click();
      cy.url().should('include', '/auth/register');
    });
  });
});
