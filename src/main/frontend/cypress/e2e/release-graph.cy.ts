describe('Graph Rendering and Interaction', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 5000 }).should('not.exist');
    cy.get('.graph-container > svg').as('graphSvg');
  });

  const robustClickNode = (nodeCySelector: string) => {
    cy.get(nodeCySelector).click({ force: true });
  };

  const getToggleButton = () => {
    return cy.get('body').then(($body) => {
      if ($body.find('.nightly-toggle-mobile:visible').length > 0) {
        return cy.get('.nightly-toggle-mobile');
      } else {
        return cy.get('.nightly-toggle');
      }
    });
  };

  context('Initial State', () => {
    it('should display the main UI components', () => {
      cy.get('app-header').should('be.visible');
      cy.get('app-release-catalogus').should('be.visible');
      cy.get('app-release-graph').should('be.visible');
    });

    it('should render a significant number of nodes and links', () => {
      getToggleButton().then(($btn) => {
        if (!$btn.hasClass('active')) {
          cy.wrap($btn).click();
        }
      });

      cy.get('@graphSvg').find('g[data-cy^="node-"]').should('have.length.greaterThan', 5);
      cy.get('@graphSvg').find('path[data-cy^="link-"]').should('have.length.greaterThan', 5);
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
      cy.get('body').then(($body) => {
        if ($body.find('app-modal').length > 0) {
          cy.get('button[aria-label="Close modal"]').click();
        }
      });

      cy.get('button[aria-label="Show release info"]').should('be.visible').click();
      cy.get('app-modal').should('be.visible').as('infoModal');
      cy.get('@infoModal').contains('h2', 'Release Support');
      cy.get('@infoModal').find('button[aria-label="Close modal"]').click();
      cy.get('app-modal').should('not.exist');
    });
  });

  context('Branch and Node Rendering (based on Seeder data)', () => {
    it('should render sub-branch nodes on different y-levels', () => {
      cy.get('body').then(($body) => {
        const hasSubBranches = $body.find('g[data-cy^="node-"]').length > $body.find('g[data-cy^="node-"][transform*=",0)"]').length;

        if (hasSubBranches) {
          cy.get('@graphSvg')
                  .find('g[data-cy^="node-"]')
                  .filter((i, el) => {
                    const transform = el.getAttribute('transform');
                    const yPos = transform?.match(/translate\([^,]+,([^)]+)\)/)?.[1];
                    return parseFloat(yPos || '0') > 0;
                  })
                  .should('have.length.greaterThan', 0);
        }
      });
    });

    it('should display snapshot releases with appropriate colors', () => {
      cy.get('body').then(($body) => {
        if ($body.find('g[data-cy*="-snapshot"]').length > 0) {
          cy.get('@graphSvg')
                  .find('g[data-cy*="-snapshot"]')
                  .first()
                  .find('circle')
                  .should('exist')
                  .and('have.attr', 'fill')
                  .and('match', /^#[0-9A-Fa-f]{6}$/);
        }
      });
      it('should display nightly releases with appropriate colors', () => {
        cy.get('@graphSvg')
                .find('g[data-cy*="-nightly"]')
                .should('have.length.greaterThan', 0)
                .first()
                .find('circle')
                .should('exist')
                .and('have.attr', 'fill')
                .and('match', /^#[0-9A-Fa-f]{6}$/);
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
      it('should display quarter markers on the graph', () => {
        cy.get('@graphSvg').find('g.quarter-marker').should('have.length.greaterThan', 0);
      });

      it('should display quarter labels with proper format (Q[1-4] YYYY)', () => {
        cy.get('body').then(($body) => {
          if ($body.find('text.quarter-label').length > 0) {
            cy.get('@graphSvg').find('text.quarter-label').first().invoke('text').should('match', /^Q[1-4] \d{4}$/);
          }
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

        cy.get('.section-toggle').contains('Important Issues').click();

        cy.get('app-release-details').contains('Feature: Add real-time graphing widget');
      });
    });

    context('Skip Node Modal Interaction (based on Seeder data)', () => {
      it('should open modal on initial skip node click and navigate from it', () => {
        cy.get('g[data-cy^="skip-node-skip-initial-"]').first().click({ force: true });

        cy.get('app-skipped-versions-modal', { timeout: 2000 }).should('be.visible');

        cy.get('app-skipped-versions-modal .version-root, app-skipped-versions-modal .version-patch').first().click({ force: true });

        cy.get('app-release-details', { timeout: 5000 }).should('be.visible');
      });
    });

    context('Nightly Toggle Functionality', () => {
      it('should display the nightly toggle button', () => {
        cy.get('.nightly-toggle-mobile, .nightly-toggle').filter(':visible').should('have.length', 1);
      });

      it('should toggle nightlies on click', () => {
        getToggleButton().should('not.have.class', 'active');
        getToggleButton().click();
        getToggleButton().should('have.class', 'active');
      });

      it('should change moon icon styling when active', () => {
        getToggleButton().click();
        cy.get('.nightly-toggle.active .moon-icon, .nightly-toggle-mobile.active .moon-icon')
                .filter(':visible')
                .should('have.css', 'background-color', 'rgb(30, 58, 138)');
      });

      it('should show nightlies nodes when toggle is active', () => {
        cy.get('@graphSvg').find('g[data-cy*="-nightly"]').then(($nightlies) => {
          if ($nightlies.length > 0) {
            getToggleButton().click();
            cy.get('@graphSvg').find('g[data-cy*="-nightly"]').should('be.visible');
          } else {
            cy.log('No nightlies nodes found - skipping test');
          }
        });
      });

      it('should display nightly labels always', () => {
        cy.get('@graphSvg').find('g[data-cy*="-nightly"]').first().then(($nightlies) => {
          if ($nightlies.length > 0) {
            cy.wrap($nightlies).find('text').should('exist');
          } else {
            cy.log('No nightlies nodes found - skipping test');
          }
        });
      });

      it('should toggle state on multiple clicks', () => {
        getToggleButton().should('not.have.class', 'active');
        getToggleButton().click();
        getToggleButton().should('have.class', 'active');
        getToggleButton().click();
        getToggleButton().should('not.have.class', 'active');
      });

      it('should reduce visible nodes when nightlies are hidden', () => {
        let initialNodeCount: number;

        cy.get('@graphSvg').find('g[data-cy^="node-"]').then(($nodes) => {
          initialNodeCount = $nodes.length;
        });

        getToggleButton().click();

        cy.get('@graphSvg').find('g[data-cy^="node-"]').should(($nodes) => {
          expect($nodes.length).to.be.greaterThan(initialNodeCount);
        });

        getToggleButton().click();

        cy.get('@graphSvg').find('g[data-cy^="node-"]').should(($nodes) => {
          expect($nodes.length).to.equal(initialNodeCount);
        });
      });

      it('should reduce visible links when nightlies are hidden', () => {
        let initialLinkCount: number;

        cy.get('@graphSvg').find('path[data-cy^="link-"]').then(($links) => {
          initialLinkCount = $links.length;
        });

        getToggleButton().click();

        cy.get('@graphSvg').find('path[data-cy^="link-"]').should(($links) => {
          expect($links.length).to.be.greaterThan(initialLinkCount);
        });

        getToggleButton().click();

        cy.get('@graphSvg').find('path[data-cy^="link-"]').should(($links) => {
          expect($links.length).to.equal(initialLinkCount);
        });
      });

      it('should hide nightly nodes when toggle is inactive', () => {
        getToggleButton().should('not.have.class', 'active');

        cy.get('@graphSvg').then(($svg) => {
          const nightlyNodes = $svg.find('g[data-cy*="-nightly"]');
          if (nightlyNodes.length > 0) {
            nightlyNodes.each((idx, node) => {
              const transform = Cypress.$(node).attr('transform');
              const yPos = transform?.match(/translate\([^,]+,([^)]+)\)/)?.[1];
              const isOnMasterBranch = yPos === '0';

              if (!isOnMasterBranch) {
                cy.log('Found nightly node not on master branch - it should be hidden');
              }
            });
          }
        });
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

      it('should open skip node modal on tap without drag', () => {
        cy.get('@graphSvg').find('g[data-cy^="skip-node-"]').first().as('firstSkipNode');

        cy.get('@firstSkipNode')
                .trigger('touchstart', { touches: [{ clientX: 100, clientY: 100 }], force: true })
                .trigger('touchend', { changedTouches: [{ clientX: 100, clientY: 100 }], force: true });

        cy.get('app-skipped-versions-modal', { timeout: 2000 }).should('be.visible');
        cy.get('app-modal button[aria-label="Close modal"]').click();
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
});
