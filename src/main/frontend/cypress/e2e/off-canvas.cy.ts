describe('Off-Canvas Panel Journey', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
    cy.get('.graph-container > svg').as('graphSvg');
  });

  it('should open, display all content correctly, and close', () => {
    cy.get('@graphSvg').find('g[data-cy^="node-v"]').first().click({ force: true });

    cy.get('app-release-off-canvas', { timeout: 10000 })
      .should('be.visible')
      .as('offCanvasPanel');
    cy.get('@offCanvasPanel')
      .find('app-loader', { timeout: 10000 })
      .should('not.exist');

    cy.get('@offCanvasPanel').within(() => {
      cy.get('h4.off-canvas-title').should('not.be.empty');

      // Check if either highlights or important issues exist, or a "no features" message
      cy.get('.off-canvas-body').should('exist').then(($body) => {
        const hasHighlights = $body.find('app-release-highlights').length > 0;
        const hasIssues = $body.find('app-release-important-issues').length > 0;
        const hasNoFeatures = $body.find('.no-new-features').length > 0;

        expect(hasHighlights || hasIssues || hasNoFeatures).to.be.true;
      });

      cy.contains('button', '✕').click();
    });

    cy.get('app-release-off-canvas').should('not.exist');
  });

  it('should allow filtering of issues within the off-canvas panel if issues exist', () => {
    cy.get('@graphSvg').find('g[data-cy^="node-v"]').first().click({ force: true });
    cy.get('app-release-off-canvas').as('offCanvas');
    cy.get('@offCanvas')
      .find('app-loader', { timeout: 10000 })
      .should('not.exist');

    // Check if important issues component exists
    cy.get('@offCanvas').then(($canvas) => {
      if ($canvas.find('app-release-important-issues').length > 0) {
        cy.get('@offCanvas')
          .find('app-release-important-issues')
          .as('importantIssues');
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
      } else {
        // If no issues component, just verify the off-canvas is displayed
        cy.get('@offCanvas').should('be.visible');
      }
    });
  });
});
