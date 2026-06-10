describe('Landing Page', () => {
  beforeEach(() => {
    // Ensure no auth token so the landing page renders (it redirects to /dashboard if authenticated)
    cy.clearLocalStorage();
    cy.visit('/');
  });

  describe('Navigation bar', () => {
    it('should display the brand name', () => {
      cy.get('.landing-nav__brand').should('contain.text', 'Howners');
    });

    it('should display nav links for Fonctionnalités, Tarifs, and FAQ', () => {
      cy.get('.landing-nav__links').within(() => {
        cy.contains('a', 'Fonctionnalités')
          .should('be.visible')
          .and('have.attr', 'href', '#features');
        cy.contains('a', 'Tarifs')
          .should('be.visible')
          .and('have.attr', 'href', '#pricing');
        cy.contains('a', 'FAQ')
          .should('be.visible')
          .and('have.attr', 'href', '#faq');
      });
    });

    it('should display "Voir les annonces" link', () => {
      cy.get('.landing-nav__listings').should('contain.text', 'Voir les annonces');
    });

    it('should display Connexion and Créer un compte CTA buttons', () => {
      cy.get('.landing-nav__cta').within(() => {
        cy.contains('button', 'Connexion').should('be.visible');
        cy.contains('button', 'Créer un compte').should('be.visible');
      });
    });
  });

  describe('Hero section', () => {
    it('should render the hero section with headline and subtext', () => {
      cy.get('.hero').should('be.visible');
      cy.get('.hero__copy h1').should('contain.text', 'La gestion locative');
      cy.get('.hero__sub').should('contain.text', 'Contrats conformes en un clic');
    });

    it('should display the hero pill with feature highlights', () => {
      cy.get('.hero__pill').should('contain.text', 'Signature canvas');
    });

    it('should render primary CTA buttons', () => {
      cy.get('.hero__ctas').within(() => {
        cy.contains('button', 'Commencer gratuitement').should('be.visible');
        cy.contains('button', 'Parcourir les annonces').should('be.visible');
      });
    });

    it('should display trust line about free tier', () => {
      cy.get('.hero__trust').should('contain.text', '2 biens et 3 contrats gratuits');
    });

    it('should render the hero visual card', () => {
      cy.get('.hero-card').should('be.visible');
      cy.get('.hero-card__title').should('contain.text', 'CTR-2026');
      cy.get('.hero-card__badge').should('contain.text', 'Signé');
    });
  });

  describe('Features section', () => {
    it('should display the features heading and 4 feature cards', () => {
      cy.get('#features').should('exist');
      cy.get('#features h2').should('contain.text', 'Tout ce qu');
      cy.get('.feature-card').should('have.length', 4);
    });

    it('should display all four feature titles', () => {
      cy.contains('.feature-card h3', 'Contrats prêts à signer').should('be.visible');
      cy.contains('.feature-card h3', 'Signature électronique').should('be.visible');
      cy.contains('.feature-card h3', 'Rentabilité visible').should('be.visible');
      cy.contains('.feature-card h3', 'Scoring locataire').should('be.visible');
    });
  });

  describe('How it works section', () => {
    it('should display 3 steps', () => {
      cy.get('.how-it-works').should('be.visible');
      cy.get('.steps li').should('have.length', 3);
      cy.contains('.steps h3', 'Créez votre bien').should('be.visible');
      cy.contains('.steps h3', 'Invitez votre locataire').should('be.visible');
      cy.contains('.steps h3', 'Générez et signez le bail').should('be.visible');
    });
  });

  describe('Social proof section', () => {
    it('should display stats counters', () => {
      cy.get('.social-proof__stats').should('be.visible');
      cy.get('.stat').should('have.length', 4);

      cy.contains('.stat__number', '500+').should('be.visible');
      cy.contains('.stat__label', 'Propriétaires actifs').should('be.visible');
      cy.contains('.stat__number', '1 200+').should('be.visible');
      cy.contains('.stat__number', '98 %').should('be.visible');
      cy.contains('.stat__number', '4.8/5').should('be.visible');
    });

    it('should display testimonials', () => {
      cy.get('.testimonial').should('have.length', 3);
      cy.contains('.testimonial__author strong', 'Sophie M.').should('be.visible');
      cy.contains('.testimonial__author strong', 'Marc D.').should('be.visible');
      cy.contains('.testimonial__author strong', 'Amina K.').should('be.visible');
    });
  });

  describe('Pricing section', () => {
    it('should render 3 pricing cards: Gratuit, Pro, Premium', () => {
      cy.get('#pricing').should('exist');
      cy.get('.plan-card').should('have.length', 3);

      cy.contains('.plan-card h3', 'Gratuit').should('be.visible');
      cy.contains('.plan-card h3', 'Pro').should('be.visible');
      cy.contains('.plan-card h3', 'Premium').should('be.visible');
    });

    it('should display correct prices', () => {
      cy.contains('.plan-card__price', '0 €').should('be.visible');
      cy.contains('.plan-card__price', '19,90 €').should('be.visible');
      cy.contains('.plan-card__price', '49,90 €').should('be.visible');
    });

    it('should highlight the Pro plan as most popular', () => {
      cy.get('.plan-card--featured').should('exist');
      cy.get('.plan-card--featured').should('contain.text', 'Le plus populaire');
      cy.get('.plan-card--featured h3').should('contain.text', 'Pro');
    });

    it('should list features for each plan', () => {
      // Gratuit plan features
      cy.contains('.plan-card h3', 'Gratuit')
        .closest('.plan-card')
        .find('.plan-card__features li')
        .should('have.length', 4);

      // Pro plan features
      cy.contains('.plan-card h3', 'Pro')
        .closest('.plan-card')
        .find('.plan-card__features li')
        .should('have.length', 5);
    });

    it('should have CTA buttons on each card', () => {
      cy.contains('.plan-card', 'Gratuit').contains('button', 'Commencer').should('be.visible');
      cy.contains('.plan-card', 'Pro').contains('button', 'Essayer Pro').should('be.visible');
      cy.contains('.plan-card', 'Premium').contains('button', 'Choisir Premium').should('be.visible');
    });
  });

  describe('FAQ section', () => {
    it('should render FAQ items', () => {
      cy.get('#faq').should('exist');
      cy.get('.faq-item').should('have.length', 4);
    });

    it('should display FAQ question summaries', () => {
      cy.contains('summary', 'Mes contrats sont-ils juridiquement opposables').should('be.visible');
      cy.contains('summary', 'Mes données sont-elles sécurisées').should('be.visible');
      cy.contains('summary', 'Puis-je migrer depuis un autre outil').should('be.visible');
      cy.contains('summary', 'Que se passe-t-il si je dépasse mon quota').should('be.visible');
    });

    it('should expand FAQ items on click', () => {
      // The <details> element's content (<p>) should not be visible until expanded
      cy.get('.faq-item').first().as('firstFaq');
      cy.get('@firstFaq').should('not.have.attr', 'open');

      // Click to open
      cy.get('@firstFaq').find('summary').click();
      cy.get('@firstFaq').should('have.attr', 'open');
      cy.get('@firstFaq').find('p').should('be.visible');
      cy.get('@firstFaq').find('p').should('contain.text', 'SHA-256');

      // Click again to close
      cy.get('@firstFaq').find('summary').click();
      cy.get('@firstFaq').should('not.have.attr', 'open');
    });
  });

  describe('Final CTA section', () => {
    it('should display the final call to action', () => {
      cy.get('.final-cta').should('be.visible');
      cy.get('.final-cta h2').should('contain.text', 'reprendre le contrôle');
      cy.contains('button', 'Démarrer gratuitement').should('be.visible');
    });
  });

  describe('Footer', () => {
    it('should render the footer with brand and legal links', () => {
      cy.get('.landing-footer').should('be.visible');
      cy.get('.landing-footer__brand').should('contain.text', 'Howners');
      cy.get('.landing-footer__legal').within(() => {
        cy.contains('a', 'Mentions légales').should('be.visible');
        cy.contains('a', 'CGU').should('be.visible');
        cy.contains('a', 'RGPD').should('be.visible');
        cy.contains('2026 Howners').should('be.visible');
      });
    });
  });
});
