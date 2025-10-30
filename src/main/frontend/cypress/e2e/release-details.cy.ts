describe('Release Details Page Journey', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
  });

  it('should navigate to release details page and display all content correctly', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });

    cy.url().should('include', '/graph/');

    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.release-details-header h2').should('contain.text', 'v9.0.1');

    cy.get('.back-button').should('be.visible').should('contain.text', 'Back');

    cy.get('app-release-highlights').should('be.visible');

    cy.get('app-release-important-issues')
      .find('app-issue-tree-branch', { timeout: 10000 })
      .should('have.length.greaterThan', 0);
  });

  it('should navigate back to graph when clicking back button', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');

    cy.get('.back-button').click();

    cy.url().should('eq', Cypress.config().baseUrl + '/graph');
    cy.get('app-release-graph').should('be.visible');
  });

  it('should close release details page when navigating to /graph', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');

    cy.visit('/graph');

    cy.get('app-release-details').should('not.exist');
    cy.url().should('eq', Cypress.config().baseUrl + '/graph');
    cy.get('app-release-graph').should('be.visible');
  });

  it('should allow filtering of issues within the release details page', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('app-release-important-issues').as('importantIssues');
    cy.get('@importantIssues')
      .find('app-issue-tree-branch')
      .its('length')
      .as('initialCount');

    cy.get('@importantIssues')
      .find('[data-cy="issue-type-filter"]')
      .select('Feature', { force: true });

    cy.get('@initialCount').then((initialCount) => {
      cy.get('@importantIssues')
        .find('app-issue-tree-branch')
        .should('not.have.length', initialCount);
    });

    cy.get('@importantIssues')
      .find('app-issue-tree-branch')
      .each(($item) => {
        cy.wrap($item).should('contain.text', 'Feature');
      });

    cy.get('@importantIssues')
      .find('[data-cy="issue-type-filter"]')
      .select('All types', { force: true });

    cy.get('@initialCount').then((initialCount) => {
      cy.get('@importantIssues')
        .find('app-issue-tree-branch')
        .should('have.length', initialCount);
    });
  });

  it('should handle navigation between different releases', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('.release-details-header h2').should('contain.text', 'v9.0.1');

    cy.get('.back-button').click();
    cy.get('app-release-graph').should('be.visible');

    cy.get('[data-cy="node-v9.0.0"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('.release-details-header h2').should('contain.text', 'v9.0.0');
  });
});
