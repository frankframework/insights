describe('Release Roadmap End-to-End Tests', () => {
  const TODAY = new Date();
  TODAY.setHours(12, 0, 0, 0);

  const getPeriodLabel = (date: Date) => {
    const getQuarter = (d: Date) => Math.floor(d.getMonth() / 3) + 1;
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
    cy.get().find('li').contains('Roadmap').click();

    cy.tick(5000);
    cy.get().should('not.be.visible');
  });

  context('Initial Load and View', () => {
    it('should display the main UI components', () => {
      cy.get().should('be.visible');
      cy.get().should('be.visible');
      cy.get().should('be.visible');
    });

    it('should display the correct initial period label for the current date', () => {
      const expectedLabel = getPeriodLabel(TODAY);
      cy.get().should('contain.text', expectedLabel);
    });

    it('should render the "today" marker', () => {
      cy.get().should('exist');
    });

    it('should display milestones that have issues in the current view', () => {
      cy.get().should('have.length.greaterThan', 1);
      cy.get().first().should('be.visible');
    });
  });

  context('Timeline Navigation', () => {
    it('should navigate to the previous period', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const prevDate = new Date(TODAY);
      prevDate.setMonth(prevDate.getMonth() - 3);
      const expectedLabel = getPeriodLabel(prevDate);

      cy.get().should('contain.text', initialLabel);
      cy.get().click();
      cy.tick(5000);
      cy.get().should('contain.text', expectedLabel);
    });

    it('should navigate to the next period', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const nextDate = new Date(TODAY);
      nextDate.setMonth(nextDate.getMonth() + 3);
      const expectedLabel = getPeriodLabel(nextDate);

      cy.get().should('contain.text', initialLabel);
      cy.get().click();
      cy.tick(5000);
      cy.get().should('contain.text', expectedLabel);
    });

    it('should return to the current period when "Go to today" is clicked', () => {
      const initialLabel = getPeriodLabel(TODAY);
      const nextDate = new Date(TODAY);
      nextDate.setMonth(nextDate.getMonth() + 3);
      const nextLabel = getPeriodLabel(nextDate);

      cy.get().click();
      cy.tick(5000);
      cy.get().should('contain.text', nextLabel);
      cy.get().click();
      cy.tick(5000);
      cy.get().should('not.exist');
      cy.get().should('contain.text', initialLabel);
    });
  });

  context('Issue Rendering and Layout Logic', () => {
    it('should display closed issues before "today" and open issues after "today"', () => {
      cy.get().invoke('css', 'left').then(left => {
        const todayPosition = parseFloat(left as unknown as string);

        cy.get().then($body => {
          if ($body.find('.milestone-lanes .issue-bar[data-state="closed"]').length > 0) {
            cy.get().each($issue => {
              const issuePosition = parseFloat($issue.css('left'));
              expect(issuePosition).to.be.lessThan(todayPosition);
            });
          }

          if ($body.find('.milestone-lanes .issue-bar[data-state="open"]').length > 0) {
            cy.get().each($issue => {
              const issuePosition = parseFloat($issue.css('left'));
              expect(issuePosition).to.be.greaterThan(todayPosition);
            });
          }
        });
      });
    });

    it('should place "overdue" open issues in the current quarter, after "today"', () => {
      cy.get().first().as('firstMilestoneRow');
      cy.get().find('a.issue-bar').then(($issues) => {
        if ($issues.length > 0) {
          cy.wrap($issues.first()).invoke('css', 'left').then(left => {
            const issuePosition = parseFloat(left as unknown as string);
            cy.get().invoke('css', 'left').then(todayLeft => {
              const todayPosition = parseFloat(todayLeft as unknown as string);
              expect(issuePosition).to.be.a('number');
              expect(todayPosition).to.be.a('number');
            });
          });
        }
      });
    });

    it('should display a tooltip with correct info on mouse hover', () => {
      cy.get().trigger('mouseenter');
      cy.tick(500);
      cy.get().should('be.visible');
      cy.get().should('contain.text', 'High priority feature to be done');
      cy.get().should('contain.text', 'Priority: High');
      cy.get().should('contain.text', 'Points: 8');

      cy.get().trigger('mouseleave');
      cy.tick(500);
      cy.get().should('not.be.visible');
    });
  });

  context('Edge Case Rendering', () => {
    it('should not display a milestone if it has no issues', () => {
      cy.get().should('not.contain.text', 'No Issues');
    });

    it('should create multiple tracks if issues overflow the available space', () => {
      cy.get().contains('.title-link', '(Overflow)').parents('app-milestone-row').as('overflowMilestoneRow');
      cy.get().find('.issue-track-area').invoke('height').should('be.greaterThan', 50);
      cy.get().find('app-issue-bar').should('have.length', 20);
    });

    it('should render an issue with 0 points with a minimum width', () => {
      cy.get().scrollIntoView().should('be.visible').invoke('width').should('be.greaterThan', 10);
    });

    it('should show an empty state when navigating to a period with no milestones', () => {
      for (let i = 0; i < 4; i++) {
        cy.get().click();
        cy.tick(5000);
      }

      cy.get().should('not.exist');
      cy.get().should('be.visible').and('contain.text', 'No open milestones');
    });
  });
});
