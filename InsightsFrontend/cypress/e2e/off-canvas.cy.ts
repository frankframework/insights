describe('Off-Canvas Panel Journey', () => {
  beforeEach(() => {
    cy.visit('/graph');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
  });

  it('should open with correct data, show details, and close again', () => {
    cy.get('[data-cy="node-v8.1.0"]').should('be.visible').click();

    cy.get('app-release-off-canvas').should('be.visible').as('offCanvas');
    cy.get('@offCanvas').find('app-loader').should('be.visible');
    cy.get('@offCanvas').find('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('@offCanvas').contains('h2', 'v8.1.0');
    cy.get('@offCanvas').find('app-release-highlights').should('be.visible');
    cy.get('@offCanvas').find('app-release-important-issues').should('be.visible').as('importantIssues');

    cy.get('@offCanvas').find('button[aria-label="Close"]').click();
    cy.get('app-release-off-canvas').should('not.exist');
  });

  it('should allow filtering of issues within the off-canvas panel', () => {
    cy.get('app-release-graph svg').trigger('wheel', { deltaY: -1500, bubbles: true });
    cy.get('[data-cy="node-v7.8.4"]').should('be.visible').click();

    cy.get('app-release-off-canvas').as('offCanvas');
    cy.get('@offCanvas').find('app-loader', { timeout: 10000 }).should('not.exist');
    cy.get('@offCanvas').find('app-release-important-issues').as('importantIssues');

    cy.get('@importantIssues').find('div.issue-item').should('have.length.greaterThan', 1);

    cy.get('@importantIssues').find('[data-cy="issue-type-filter"]').select('Bug');

    cy.get('@importantIssues').find('div.issue-item').each(($item) => {
      cy.wrap($item).should('contain.text', 'Bug');
    });

    cy.get('@importantIssues').find('[data-cy="issue-type-filter"]').select('All types');
    cy.get('@importantIssues').find('div.issue-item').should('have.length.greaterThan', 1);
  });
});
