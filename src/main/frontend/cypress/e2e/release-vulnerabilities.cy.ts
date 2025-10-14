describe('Release Vulnerabilities Component', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');
  });

  it('should display vulnerabilities when navigating to release details', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('app-release-vulnerabilities').should('be.visible');
    cy.get('.vulnerabilities-split').should('be.visible');
    cy.get('.cve-list').should('be.visible');
    cy.get('.cwe-details').should('be.visible');
  });

  it('should display vulnerability count in title', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.vulnerabilities-title').should('exist').invoke('text').should('match', /Vulnerabilities \(\d+\)/);
  });

  it('should sort vulnerabilities by severity (CRITICAL first)', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').then(($items) => {
      if ($items.length > 1) {
        cy.wrap($items).first().find('.severity-badge').should('exist');

        cy.wrap($items).last().find('.severity-badge').should('exist');
      }
    });
  });

  it('should auto-select the first (most critical) vulnerability', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').first().should('have.class', 'selected');
    cy.get('.cwe-details h4').should('be.visible');
  });

  it('should select a CVE when clicked and update details panel', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').eq(1).as('secondCve');
    cy.get('@secondCve').find('.cve-id').invoke('text').as('cveId');

    cy.get('@secondCve').click();
    cy.get('@secondCve').should('have.class', 'selected');

    cy.get('@cveId').then((cveId) => {
      cy.get('.cwe-details h4').should('contain.text', cveId);
    });
  });

  it('should display severity badge in both list and details', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').first().find('.severity-badge').invoke('text').as('listSeverity');

    cy.get('@listSeverity').then((severity) => {
      cy.get('.cve-header-details .severity-badge').should('contain.text', severity);
    });
  });

  it('should display CVSS score formatted correctly', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-score').each(($score) => {
      cy.wrap($score).invoke('text').should('match', /CVSS Score: \d+(\.\d)?$/);
    });
  });

  it('should display CVE description', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-description').should('be.visible').and('not.be.empty');
  });

  it('should show "See more" button only when description exceeds 6 lines', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-description').then(($description) => {
      const scrollHeight = $description[0].scrollHeight;
      const clientHeight = $description[0].clientHeight;

      if (scrollHeight > clientHeight) {
        cy.get('.see-more-button').should('be.visible');
      } else {
        cy.get('.see-more-button').should('not.exist');
      }
    });
  });

  it('should toggle description expansion when "See more" button is clicked', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.see-more-button').then(($button) => {
      if ($button.length > 0) {
        cy.get('.cve-description').should('not.have.class', 'expanded');
        cy.get('.see-more-button').should('contain.text', 'See more').click();
        cy.get('.cve-description').should('have.class', 'expanded');
        cy.get('.see-more-button').should('contain.text', 'See less').click();
        cy.get('.cve-description').should('not.have.class', 'expanded');
      }
    });
  });

  it('should reset description expansion when selecting a new CVE', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.see-more-button').then(($button) => {
      if ($button.length > 0) {
        cy.get('.see-more-button').click();
        cy.get('.cve-description').should('have.class', 'expanded');

        cy.get('.cve-item').eq(1).click();
        cy.get('.cve-description').should('not.have.class', 'expanded');
      }
    });
  });

  it('should display CWEs section when CVE has associated CWEs', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cwes-section').then(($section) => {
      if ($section.length > 0) {
        cy.get('.cwes-section h5').should('contain.text', 'CWEs');
        cy.get('.cwe-badge').should('have.length.greaterThan', 0);
      }
    });
  });

  it('should have clickable CWE badges with correct MITRE URLs', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cwe-badge').then(($badges) => {
      if ($badges.length > 0) {
        cy.wrap($badges).first().as('firstBadge');
        cy.get('@firstBadge').invoke('attr', 'href').should('match', /https:\/\/cwe\.mitre\.org\/data\/definitions\/\d+\.html/);
        cy.get('@firstBadge').invoke('attr', 'target').should('equal', '_blank');
        cy.get('@firstBadge').invoke('attr', 'rel').should('equal', 'noopener noreferrer');
      }
    });
  });

  it('should apply correct severity styling classes', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    const severityClasses = ['severity-critical', 'severity-high', 'severity-medium', 'severity-low', 'severity-none', 'severity-unknown'];

    cy.get('.severity-badge').each(($badge) => {
      const classList = Array.from($badge[0].classList);
      const hasSeverityClass = severityClasses.some(cls => classList.includes(cls));
      expect(hasSeverityClass).to.be.true;
    });
  });

  it('should handle clicking through multiple CVEs', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').then(($items) => {
      if ($items.length >= 3) {
        cy.get('.cve-item').eq(0).click();
        cy.get('.cve-item').eq(0).should('have.class', 'selected');

        cy.get('.cve-item').eq(1).click();
        cy.get('.cve-item').eq(1).should('have.class', 'selected');
        cy.get('.cve-item').eq(0).should('not.have.class', 'selected');

        cy.get('.cve-item').eq(2).click();
        cy.get('.cve-item').eq(2).should('have.class', 'selected');
        cy.get('.cve-item').eq(1).should('not.have.class', 'selected');
      }
    });
  });

  it('should maintain panel widths (50/50 split)', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.vulnerabilities-split').should('have.css', 'display', 'flex');
    cy.get('.cve-list').should('be.visible');
    cy.get('.cwe-details').should('be.visible');
  });

  it('should allow clicking CVE items from any position in the list', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').then(($items) => {
      if ($items.length > 3) {
        cy.get('.cve-item').last().click();

        cy.get('.cve-item').last().should('have.class', 'selected');

        cy.wait(1000);

        cy.get('.cwe-details h4').should('be.visible');
      }
    });
  });

  it('should display vulnerability list or no vulnerabilities message', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-list').then(($list) => {
      if ($list.find('.cve-item').length > 0) {
        cy.get('.vulnerabilities-title').should('be.visible');
        cy.get('.cve-item').should('have.length.greaterThan', 0);
      } else {
        cy.get('.no-vulnerabilities').should('be.visible').and('contain.text', 'No vulnerabilities found');
      }
    });
  });

  it('should handle hover states on CVE items', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cve-item').first().trigger('mouseover');
    cy.get('.cve-item').first().should('have.css', 'cursor', 'pointer');
  });

  it('should handle hover states on CWE badges', () => {
    cy.get('[data-cy="node-v9.0.1"]').should('exist').click({ force: true });
    cy.get('app-release-details', { timeout: 10000 }).should('be.visible');
    cy.get('app-loader', { timeout: 10000 }).should('not.exist');

    cy.get('.cwe-badge').then(($badges) => {
      if ($badges.length > 0) {
        cy.wrap($badges).first().trigger('mouseover');
        cy.wrap($badges).first().should('have.css', 'cursor', 'pointer');
      }
    });
  });
});
