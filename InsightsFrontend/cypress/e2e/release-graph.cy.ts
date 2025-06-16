describe('Graph Rendering and Interaction', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
    cy.get('app-release-graph svg').should('be.visible').as('graphSvg');
  });

  context('Initial State', () => {
    it('should display the main UI components', () => {
      cy.get('app-tab-header').should('be.visible');
      cy.get('app-release-catalogus').should('be.visible');
      cy.get('app-release-graph').should('be.visible');
    });

    it('should render a significant number of nodes and links', () => {
      cy.get('@graphSvg').find('g[data-cy^="node-"]').should('have.length.greaterThan', 20);
      cy.get('@graphSvg').find('path[data-cy^="link-"]').should('have.length.greaterThan', 20);
    });

    it('should display the most recent releases on the right side of the view', () => {
      cy.get('[data-cy="node-v9.1.1-20250612.022341 (nightly)"]').should('be.visible');
    });
  });

  context('Graph Interaction', () => {
    it('should pan the graph horizontally on mouse wheel scroll', () => {
      let initialTransform: string | undefined;

      cy.get('@graphSvg').find('> g').invoke('attr', 'transform').then((transform) => {
        initialTransform = transform;
      });

      cy.get('@graphSvg').trigger('wheel', { deltaY: 500, bubbles: true });

      cy.get('@graphSvg').find('> g').invoke('attr', 'transform').should((newTransform) => {
        expect(newTransform).not.to.equal(initialTransform);
      });
    });
  });
});
