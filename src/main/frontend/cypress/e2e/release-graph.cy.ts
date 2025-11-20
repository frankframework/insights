describe('Graph Rendering and Interaction', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 5000 }).should('not.exist');
    cy.get('.graph-container > svg').as('graphSvg');
  });

  const robustClickNode = (nodeCySelector: string) => {
    cy.get(nodeCySelector).click({ force: true });
  };

  context('Initial State', () => {
    it('should display the main UI components', () => {
      cy.get('app-header').should('be.visible');
      cy.get('app-release-catalogus').should('be.visible');
      cy.get('app-release-graph').should('be.visible');
    });

    it('should render a significant number of nodes and links', () => {
      cy.get('@graphSvg').find('g[data-cy^="node-"]').should('have.length.greaterThan', 13);
      cy.get('@graphSvg').find('path[data-cy^="link-"]').should('have.length.greaterThan', 13);
    });

    it('should display releases on the graph', () => {
      cy.get('@graphSvg').find('g[data-cy^="node-v"]').first().should('exist');
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
      cy.get('@infoModal').find('button[aria-label="Close modal"]').click();
      cy.get('app-modal').should('not.exist');
    });
  });

  context('Branch and Node Rendering (based on Seeder data)', () => {
    it('should render sub-branch nodes on different y-levels', () => {
      cy.get('@graphSvg')
        .find('g[data-cy^="node-v"]')
        .filter((i, el) => {
          const transform = el.getAttribute('transform');
          return transform?.match(/translate\([^,]+,\s*0\)/) !== null;
        })
        .should('have.length.greaterThan', 0);

      cy.get('@graphSvg')
        .find('g[data-cy^="node-v"]')
        .filter((i, el) => {
          const transform = el.getAttribute('transform');
          const yPos = transform?.match(/translate\([^,]+,([^)]+)\)/)?.[1];
          return parseFloat(yPos || '0') > 0;
        })
        .should('have.length.greaterThan', 0);
    });

    it('should display nightly releases with darkblue color', () => {
      cy.get('@graphSvg').find('g[data-cy="node-9.1-snapshot"]')
        .find('circle[fill="darkblue"]')
        .should('exist');
    });
  });

  context('Skip Node Functionality (based on Seeder data)', () => {
    it('should display skip nodes for version gaps', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').should('have.length.greaterThan', 0);
    });

    it('should display an initial skip node (for v6.1)', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-skip-initial-"]').should('have.length.greaterThan', 0);
    });

    it('should display dotted links to and from skip nodes', () => {
      cy.get('@graphSvg').find('path.dotted').should('have.length.greaterThan', 0);
    });
  });

  context('Timeline and Quarter Markers', () => {
    it('should display multiple quarter markers on the graph', () => {
      cy.get('@graphSvg').find('g.quarter-marker').should('have.length.greaterThan', 5);
    });

    it('should display quarter labels with proper format (Q[1-4] YYYY)', () => {
      cy.get('@graphSvg').find('text.quarter-label').first().invoke('text').should('match', /^Q[1-4] \d{4}$/);
    });
  });

  context('Cluster Functionality (based on Seeder data)', () => {
    it('should display cluster nodes as recent releases are grouped', () => {
      cy.get('@graphSvg').find('g.cluster-node').should('have.length.greaterThan', 0);
    });

    it('should expand cluster on click', () => {
      cy.get('@graphSvg').find('g[data-cy^="node-v"]').its('length').then((initialNodeCount) => {
        cy.get('@graphSvg').find('g.cluster-node').first().as('clusterNode').click({ force: true });

        cy.wait(50);

        cy.get('@graphSvg').find('g[data-cy^="node-v"]').should('have.length.greaterThan', initialNodeCount);

        cy.get('@graphSvg').find('g.collapse-button').should('be.visible');
      });
    });
  });

  context('Release Details Navigation (based on Seeder data)', () => {
    it('should navigate to release details on node click', () => {
      robustClickNode('[data-cy="node-v9.0.1"]');

      cy.url().should('include', '/graph/RE_kwDOAIg5ds4MnUo_');
      cy.get('app-release-details', { timeout: 5000 }).should('be.visible');

      cy.get('app-release-details').contains('v9.0.1');
      cy.get('app-release-details').contains('CVE-2024-0001');
      cy.get('app-release-details').contains('Feature: Add real-time graphing widget');
    });
  });

  context('Skip Node Modal Interaction (based on Seeder data)', () => {
    it('should open modal on initial skip node click and navigate from it', () => {
      cy.get('g[data-cy^="skip-node-skip-initial-"]').first().click({ force: true });

      cy.get('app-skipped-versions-modal', { timeout: 2000 }).should('be.visible');

      cy.get('app-skipped-versions-modal').contains('v6.1').should('be.visible');

      cy.get('app-skipped-versions-modal').contains('v6.1').click({force: true});

      cy.url().should('include', '/graph/MDc6UmVsZWFzZTQ5MDUxNjU%3D');
      cy.get('app-release-details', { timeout: 5000 }).should('be.visible');
      cy.get('app-release-details').contains('v6.1');
    });
  });

  context('Touch Events on Mobile', () => {
    it('should pan the graph on touch swipe gesture', () => {
      let initialTransform: string | undefined;

      cy.get('@graphSvg').first().find('> g').invoke('attr', 'transform').then((transform) => {
        initialTransform = transform;
      });

      cy.get('@graphSvg').first()
        .trigger('touchstart', { touches: [{ clientX: 300, clientY: 200 }] })
        .trigger('touchmove', { touches: [{ clientX: 200, clientY: 200 }] })
        .trigger('touchend');

      cy.get('@graphSvg').first().find('> g').invoke('attr', 'transform').should((newTransform) => {
        expect(newTransform).not.to.equal(initialTransform);
      });
    });

    it('should open release node details on tap without drag', () => {
      cy.get('@graphSvg').find('g[data-cy="node-v9.0.1"]').as('firstNode');

      cy.get('@firstNode')
        .trigger('touchstart', { touches: [{ clientX: 100, clientY: 100 }], force: true })
        .trigger('touchend', { changedTouches: [{ clientX: 100, clientY: 100 }], force: true });

      cy.url().should('include', '/graph/RE_kwDOAIg5ds4MnUo_');
      cy.get('app-release-details', { timeout: 5000 }).should('be.visible');
    });

    it('should open skip node modal on tap without drag', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('firstSkipNode');

      cy.get('@firstSkipNode')
        .trigger('touchstart', { touches: [{ clientX: 100, clientY: 100 }], force: true })
        .trigger('touchend', { changedTouches: [{ clientX: 100, clientY: 100 }], force: true });

      cy.get('app-skipped-versions-modal', { timeout: 2000 }).should('be.visible');
    });

    it('should not open skip node modal when dragging over skip node', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('firstSkipNode');

      cy.get('@firstSkipNode')
        .trigger('touchstart', { touches: [{ clientX: 100, clientY: 100 }], force: true })
        .trigger('touchmove', { touches: [{ clientX: 150, clientY: 100 }], force: true })
        .trigger('touchend', { changedTouches: [{ clientX: 150, clientY: 100 }], force: true });

      cy.get('app-skipped-versions-modal').should('not.exist');
    });
  });
});
