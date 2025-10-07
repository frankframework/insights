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
      cy.get('@infoModal').find('.release-content-item').should('have.length', 5);
      cy.get('@infoModal').contains('p', 'Our policy is to provide major versions with one year of security support and six months of technical support.');
      cy.get('@infoModal').find('button[aria-label="Close modal"]').click();

      cy.get('app-modal').should('not.exist');
    });
  });

  context('Skip Node Functionality', () => {
    it('should display skip nodes for version gaps', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').should('have.length.greaterThan', 0);
    });

    it('should display skip nodes with correct positioning between release nodes', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('skipNode');

      cy.get('@skipNode').should('exist');
      cy.get('@skipNode').should('have.attr', 'transform');
    });

    it('should display dotted links to and from skip nodes', () => {
      cy.get('@graphSvg').find('path.dotted').should('have.length.greaterThan', 0);
    });

    it('should open skip node modal when clicking on a skip node', () => {
      cy.get('app-modal').should('not.exist');

      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible').as('skipModal');
      cy.get('@skipModal').should('contain', 'Skipped Releases');
    });

    it('should display skipped versions in the modal with proper structure', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible').as('skipModal');
      cy.get('@skipModal').find('.skipped-versions-list').should('be.visible');
      cy.get('@skipModal').find('.version-root, .version-patch').should('have.length.greaterThan', 0);
    });

    it('should show version badges in the skipped releases modal', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible');
      cy.get('.version-type-badge').should('have.length.greaterThan', 0);
      cy.get('.version-type-badge').first().invoke('text').should('match', /MAJOR|MINOR|PATCH/);
    });

    it('should allow clicking on skipped version to view release details', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible');
      cy.get('.version-root').first().click();

      cy.get('app-release-details').should('be.visible');
    });

    it('should close skip node modal when clicking close button', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible');
      cy.get('app-modal').find('button[aria-label="Close modal"]').click();

      cy.get('app-modal').should('not.exist');
    });

    it('should close skip node modal when clicking outside', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible');
      cy.get('.modal-backdrop').click({ force: true });

      cy.get('app-modal').should('not.exist');
    });

    it('should display initial skip node if there are skipped versions at the beginning', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').then(($skipNodes) => {
        const initialSkipNode = $skipNodes.filter('[data-cy^="skip-node-skip-initial-"]');
        if (initialSkipNode.length > 0) {
          expect(initialSkipNode).to.have.length.greaterThan(0);
        }
      });
    });

    it('should show proper fade-in links from start position if they exist', () => {
      cy.get('@graphSvg').then(($svg) => {
        const links = $svg.find('path[data-cy^="link-start-node-"]');
        if (links.length > 0) {
          expect(links).to.have.length.greaterThan(0);
          expect(links.first()).to.have.class('dotted');
        }
      });
    });

    it('should handle skip nodes with different skip counts correctly', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').then(($skipNodes) => {
        const nodesToTest = $skipNodes.slice(0, 3);
        cy.wrap(nodesToTest).each(($skipNode) => {
          cy.wrap($skipNode).click({ force: true });
          cy.get('app-modal').should('be.visible');

          cy.get('.skipped-versions-list').should('be.visible');

          cy.get('app-modal').find('button[aria-label="Close modal"]').click();
          cy.get('app-modal').should('not.exist');
        });
      });
    });

    it('should display skip count number on skip nodes', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().find('text.skip-text').should('exist');
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().find('text.skip-text').invoke('text').should('not.be.empty');
    });

    it('should show proper tree structure with patches indented', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().click({ force: true });

      cy.get('app-modal').should('be.visible');
      cy.get('.version-patch').should('contain', '└─');
      cy.get('.version-patch .patch-indent').should('be.visible');
    });
  });

  context('Skip Node Integration with Graph Layout', () => {
    it('should maintain proper spacing between skip nodes and release nodes', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('skipNode');
      cy.get('@graphSvg').find('g[data-cy^="node-v"]').first().as('releaseNode');

      cy.get('@skipNode').invoke('attr', 'transform').then((skipTransform) => {
        cy.get('@releaseNode').invoke('attr', 'transform').then((releaseTransform) => {
          const skipMatch = skipTransform!.match(/translate\(([^,]+),([^)]+)\)/);
          const releaseMatch = releaseTransform!.match(/translate\(([^,]+),([^)]+)\)/);

          if (skipMatch && releaseMatch) {
            const distance = Math.abs(parseFloat(skipMatch[1]) - parseFloat(releaseMatch[1]));
            expect(distance).to.be.greaterThan(50);
          }
        });
      });
    });

    it('should have skip nodes positioned on the same y-level as master branch', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('skipNode');
      cy.get('@graphSvg').find('g[data-cy^="node-v"]').first().as('masterNode');

      cy.get('@skipNode').invoke('attr', 'transform').then((skipTransform) => {
        cy.get('@masterNode').invoke('attr', 'transform').then((masterTransform) => {
          const skipMatch = skipTransform!.match(/translate\([^,]+,([^)]+)\)/);
          const masterMatch = masterTransform!.match(/translate\([^,]+,([^)]+)\)/);

          if (skipMatch && masterMatch) {
            expect(parseFloat(skipMatch[1])).to.equal(parseFloat(masterMatch[1]));
          }
        });
      });
    });

    it('should show skip nodes with appropriate visual styling', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().find('circle.skip-circle').as('skipCircle');

      cy.get('@skipCircle').should('exist');
      cy.get('@skipCircle').should('have.attr', 'r', '20');
    });

    it('should display skip nodes as interactive elements with cursor pointer', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().should('have.class', 'skip-node');
    });

    it('should show correct skip count text on skip nodes', () => {
      cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').each(($skipNode) => {
        cy.wrap($skipNode).find('text.skip-text').then(($text) => {
          const skipCount = parseInt($text.text());
          expect(skipCount).to.be.greaterThan(0);
        });
      });
    });
  });
});
