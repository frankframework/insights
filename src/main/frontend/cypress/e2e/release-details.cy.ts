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

  it('should support direct URL navigation to release details', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });

    cy.url().then((url) => {
      const releaseUrl = url;

      cy.visit('/graph');
      cy.get('app-loader', { timeout: 10000 }).should('not.exist');

      cy.visit(releaseUrl);
      cy.get('app-loader', { timeout: 10000 }).should('not.exist');

      cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
      cy.get('.release-details-header h2').should('contain.text', 'v9.0.1');
      cy.get('app-release-highlights').should('be.visible');
    });
  });

  it('should display proper loading state while fetching release data', () => {
    cy.intercept('GET', '**/releases/*', (req) => {
      req.reply((res) => {
        res.delay = 1000;
      });
    }).as('getReleaseById');

    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });

    cy.get('app-loader', { timeout: 10000 }).should('be.visible');

    cy.wait('@getReleaseById');

    cy.get('app-loader').should('not.exist');
    cy.get('app-release-details').should('be.visible');
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

  it('should display error message when release is not found', () => {
    cy.visit('/graph/non-existent-release-id');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.toast-error').should('be.visible').should('contain.text', 'Failed to load release');
  });

  it('should show error when no highlights are found', () => {
    cy.intercept('GET', '**/releases/*/highlights', {
      statusCode: 200,
      body: [],
    }).as('getEmptyHighlights');

    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.wait('@getEmptyHighlights');

    cy.get('.toast-error').should('be.visible').should('contain.text', 'No release highlights found');
  });

  it('should show error when no issues are found', () => {
    cy.intercept('GET', '**/releases/*/issues', {
      statusCode: 200,
      body: [],
    }).as('getEmptyIssues');

    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.wait('@getEmptyIssues');

    cy.get('.toast-error').should('be.visible').should('contain.text', 'No release issues found');
  });
});
