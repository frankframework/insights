describe('Application Routing', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-header').find('li').contains('Roadmap').click();
  });

  it('should navigate between Graph and Roadmap pages using the tab header', () => {
    cy.url().should('include', '/roadmap');
    cy.get('app-roadmap').should('be.visible');
    cy.get('app-release-graph').should('not.exist');

    cy.get('app-header').find('li').contains('Release graph').click();
    cy.url().should('not.include', '/roadmap');
    cy.get('app-release-graph').should('be.visible');
    cy.get('app-roadmap').should('not.exist');
  });
});
