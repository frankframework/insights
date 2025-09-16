describe('Off-Canvas Panel Journey', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
    cy.get('.graph-container > svg').as('graphSvg');
  });

  it('should open with correct data, show details, and close again', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('be.visible').click();

    cy.get('app-release-off-canvas').should('be.visible').as('offCanvas');
    cy.get('@offCanvas').find('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('@offCanvas').find('h4.off-canvas-title').should('contain.text', 'v9.0.1');

    cy.get('@offCanvas').find('app-release-highlights').should('be.visible');
    cy.get('@offCanvas').find('app-release-important-issues').should('be.visible');

    cy.get('@offCanvas').contains('button', '✕').click();
    cy.get('app-release-off-canvas').should('not.exist');
  });

  it('should allow filtering of issues within the off-canvas panel', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('be.visible').click();
    cy.get('app-release-off-canvas').as('offCanvas');
    cy.get('@offCanvas').find('app-loader', { timeout: 10000 }).should('not.exist');
    cy.get('@offCanvas').find('app-release-important-issues').as('importantIssues');
    cy.get('@importantIssues').find('app-issue-tree-branch').its('length').as('initialCount');
    cy.get('@importantIssues').find('[data-cy="issue-type-filter"]').select('Feature');

    cy.get('@initialCount').then(initialCount => {
      cy.get('@importantIssues').find('app-issue-tree-branch').should('not.have.length', initialCount);
    });

    cy.get('@importantIssues').find('app-issue-tree-branch').each(($item) => {
      cy.wrap($item).should('contain.text', 'Feature');
    });

    cy.get('@importantIssues').find('[data-cy="issue-type-filter"]').select('All types');

    cy.get('@initialCount').then(initialCount => {
      cy.get('@importantIssues').find('app-issue-tree-branch').should('have.length', initialCount);
    });
  });
});
