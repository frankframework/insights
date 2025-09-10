describe('Graph Rendering and Interaction', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 5000 }).should('not.exist');
    cy.get('.graph-container > svg').as('graphSvg');
  });

  context('Initial State', () => {
    it('should display the main UI components', () => {
      cy.get('app-header').should('be.visible');
      cy.get('app-release-catalogus').should('be.visible');
      cy.get('app-release-graph').should('be.visible');
    });

    it('should render a significant number of nodes and links', () => {
      cy.get('@graphSvg').find('g[data-cy^="node-"]').should('have.length.greaterThan', 15);
      cy.get('@graphSvg').find('path[data-cy^="link-"]').should('have.length.greaterThan', 15);
    });

    it('should display the most recent releases on the right side of the view', () => {
      cy.get('[data-cy="node-v9.1.1-nightly"]').should('be.visible');
    });

    it('should hide minor releases of unsupported majors, but keep the major visible', () => {
      cy.get('[data-cy="node-v7.4"]')
        .should('be.visible')
        .find('circle')
        .should('have.attr', 'fill', '#FD230E');

      cy.get('[data-cy="node-v7.9.4"]').should('not.exist');
    });
  });

  context('Graph Interaction', () => {
    it('should pan the graph on mouse wheel scroll', () => {
      let initialTransform: string | undefined;

      cy.get('@graphSvg').first().find('> g').invoke('attr', 'transform').then((transform) => {
        initialTransform = transform;
      });

      cy.get('@graphSvg').first().trigger('wheel', { deltaY: 500, bubbles: true });

      cy.get('@graphSvg').first().find('> g').invoke('attr', 'transform').should((newTransform) => {
        expect(newTransform).not.to.equal(initialTransform);
      });
    });

    it('should open and close the release support info modal', () => {
      cy.get('app-modal').should('not.exist');

      cy.get('button[aria-label="Show release info"]').should('be.visible').click();

      cy.get('app-modal').should('be.visible').as('infoModal');

      cy.get('@infoModal').contains('h2', 'Release Support');
      cy.get('@infoModal').find('.release-content-item').should('have.length', 4);
      cy.get('@infoModal').contains('p', 'Our policy is to provide major versions with one year of security support and six months of technical support.');
      cy.get('@infoModal').find('button[aria-label="Close modal"]').click();

      cy.get('app-modal').should('not.exist');
    });
  });
});
