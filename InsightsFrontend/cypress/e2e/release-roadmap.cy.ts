describe('Release Roadmap End-to-End Tests', () => {
  const TODAY = new Date();
  TODAY.setHours(12, 0, 0, 0);

  const getPeriodLabel = (date) => {
    const getQuarter = (d) => Math.floor(d.getMonth() / 3) + 1;
    const startYear = date.getFullYear();
    const startQuarter = getQuarter(date);

    const endDate = new Date(date);
    endDate.setMonth(endDate.getMonth() + 3);
    const endYear = endDate.getFullYear();
    const endQuarter = getQuarter(endDate);

    return `Q${startQuarter} ${startYear} - Q${endQuarter} ${endYear}`;
  };


  beforeEach(() => {
    cy.clock(TODAY.getTime(), ['Date', 'setTimeout', 'clearTimeout', 'setInterval', 'clearInterval']);

    cy.visit('/');
    cy.get('app-header').find('li').contains('Roadmap').click();

    cy.tick(5000);
    cy.get('app-loader').should('not.be.visible');
  });

  context('Initial Load and View', () => {
    it('should display the main UI components', () => {
      cy.get('app-roadmap-toolbar').should('be.visible');
      cy.get('app-timeline-header').should('be.visible');
      cy.get('.milestone-lanes').should('be.visible');
    });

    it('should display the correct initial period label for the current date', () => {
      const expectedLabel = getPeriodLabel(TODAY);
      cy.get('.period-label').should('contain.text', expectedLabel);
    });

    it('should render the "today" marker', () => {
      cy.get('.today-marker').should('be.visible');
    });

    it('should display milestones that have issues in the current view', () => {
      cy.get('app-milestone-row').should('have.length.greaterThan', 1);
      cy.get('app-milestone-row').first().should('be.visible');
    });
  });

  context('Timeline Navigation', () => {
    it('should navigate to the previous period', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const prevDate = new Date(TODAY);
      prevDate.setMonth(prevDate.getMonth() - 3);
      const expectedLabel = getPeriodLabel(prevDate);

      cy.get('.period-label').should('contain.text', initialLabel);
      cy.get('button[title="Previous quarter"]').click();
      cy.tick(5000);
      cy.get('.period-label').should('contain.text', expectedLabel);
    });

    it('should navigate to the next period', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const nextDate = new Date(TODAY);
      nextDate.setMonth(nextDate.getMonth() + 3);
      const expectedLabel = getPeriodLabel(nextDate);

      cy.get('.period-label').should('contain.text', initialLabel);
      cy.get('button[title="Next quarter"]').click();
      cy.tick(5000);
      cy.get('.period-label').should('contain.text', expectedLabel);
    });

    it('should return to the current period when "Go to today" is clicked', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const nextDate = new Date(TODAY);
      nextDate.setMonth(nextDate.getMonth() + 3);
      const nextLabel = getPeriodLabel(nextDate);

      cy.get('button[title="Next quarter"]').click();
      cy.tick(5000);
      cy.get('.period-label').should('contain.text', nextLabel);
      cy.get('button[title="Go to today"]').click();
      cy.tick(5000);
      cy.get('app-loader').should('not.exist');
      cy.get('.period-label').should('contain.text', initialLabel);
    });
  });

  context('Issue Rendering and Layout Logic', () => {
    it('should display closed issues before "today" and open issues after "today"', () => {
      cy.get('app-milestone-row').eq(1).as('firstMilestoneRow');
      cy.get('.today-marker').invoke('css', 'left').then(left => {
        const todayPosition = parseFloat(left as unknown as string);

        cy.get('@firstMilestoneRow')
          .find('a.issue-bar[href*="203"]')
          .invoke('css', 'left')
          .then((pos) => expect(parseFloat(pos as unknown as string)).to.be.lessThan(todayPosition));
        cy.get('@firstMilestoneRow')
          .find('a.issue-bar[href*="204"]')
          .invoke('css', 'left')
          .then((pos) => expect(parseFloat(pos as unknown as string)).to.be.lessThan(todayPosition));

        cy.get('@firstMilestoneRow')
          .find('a.issue-bar[href*="205"]')
          .invoke('css', 'left')
          .then((pos) => expect(parseFloat(pos as unknown as string)).to.be.greaterThan(todayPosition));
        cy.get('@firstMilestoneRow')
          .find('a.issue-bar[href*="206"]')
          .invoke('css', 'left')
          .then((pos) => expect(parseFloat(pos as unknown as string)).to.be.greaterThan(todayPosition));
        cy.get('@firstMilestoneRow')
          .find('a.issue-bar[href*="207"]')
          .invoke('css', 'left')
          .then((pos) => expect(parseFloat(pos as unknown as string)).to.be.greaterThan(todayPosition));
      });
    });

    it('should place "overdue" open issues in the current quarter, after "today"', () => {
      cy.get('app-milestone-row').first().as('firstMilestoneRow');
      const overdueIssue = cy.get('@firstMilestoneRow').find('a.issue-bar[href*="202"]').should('be.visible');
      overdueIssue.invoke('css', 'left').then(left => {
        const issuePosition = parseFloat(left as unknown as string);
        cy.get('.today-marker').invoke('css', 'left').then(todayLeft => {
          const todayPosition = parseFloat(todayLeft as unknown as string);
          expect(issuePosition).to.be.greaterThan(todayPosition);
        });
      });
    });

    it('should display a tooltip with correct info on mouse hover', () => {
      cy.get('a.issue-bar[href*="205"]').trigger('mouseenter');
      cy.tick(500);
      cy.get('app-tooltip').should('be.visible');
      cy.get('.tooltip-title').should('contain.text', 'High priority feature to be done');
      cy.get('.tooltip-detail').should('contain.text', 'Priority: High');
      cy.get('.tooltip-detail').should('contain.text', 'Points: 8');

      cy.get('a.issue-bar[href*="205"]').trigger('mouseleave');
      cy.tick(500);
      cy.get('app-tooltip').should('not.be.visible');
    });
  });

  context('Edge Case Rendering', () => {
    it('should not display a milestone if it has no issues', () => {
      cy.get('app-milestone-row .title-link').should('not.contain.text', 'No Issues');
    });

    it('should create multiple tracks if issues overflow the available space', () => {
      cy.get('app-milestone-row').contains('.title-link', '(Overflow)').parents('app-milestone-row').as('overflowMilestoneRow');
      cy.get('@overflowMilestoneRow').find('.issue-track-area').invoke('height').should('be.greaterThan', 50);
      cy.get('@overflowMilestoneRow').find('app-issue-bar').should('have.length', 20);
    });

    it('should render an issue with 0 points with a minimum width', () => {
      cy.get('a.issue-bar[href*="207"]').scrollIntoView().should('be.visible').invoke('width').should('be.greaterThan', 10);
    });

    it('should show an empty state when navigating to a period with no milestones', () => {
      for (let i = 0; i < 4; i++) {
        cy.get('button[title="Next quarter"]').click();
        cy.tick(5000);
      }

      cy.get('app-loader').should('not.exist');
      cy.get('.empty-state').should('be.visible').and('contain.text', 'No open milestones');
    });
  });
});
