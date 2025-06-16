describe('Application Routing', () => {
  it('should redirect from the root path ("/") to "/graph"', () => {
    cy.visit('/');
    cy.url().should('include', '/graph');
    cy.get('app-release-graph').should('be.visible');
  });

  it('should navigate between Graph and Roadmap pages using the tab header', () => {
    cy.visit('/graph'); // Start op de graph pagina

    cy.get('app-tab-header').contains('a', 'Roadmap').click();
    cy.url().should('include', '/roadmap');
    cy.get('app-roadmap').should('be.visible');
    cy.get('app-release-graph').should('not.exist');

    cy.get('app-tab-header').contains('a', 'Graph').click();
    cy.url().should('include', '/graph');
    cy.get('app-release-graph').should('be.visible');
    cy.get('app-roadmap').should('not.exist');
  });

  it('should redirect to not-found page for an invalid URL', () => {
    cy.visit('/this-url-does-not-exist', { failOnStatusCode: false });
    cy.url().should('include', '/not-found');
    cy.get('app-not-found').should('be.visible');
  });
});
