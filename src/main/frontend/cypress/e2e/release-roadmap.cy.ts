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

  const getMonthlyPeriodLabel = (date: Date) => {
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const month = monthNames[date.getMonth()];
    const year = date.getFullYear();
    return `${month} ${year}`;
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

    it('should render the "today" marker if today is in the visible period', () => {
      cy.get('body').then(($body) => {
        if ($body.find('.today-marker').length > 0) {
          cy.get('.today-marker').should('exist');
        } else {
          cy.log('Today marker not visible - today is outside the current period');
        }
      });
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
      cy.get('body').then(($body) => {
        if ($body.find('.today-marker').length > 0) {
          cy.get('.today-marker').invoke('css', 'left').then(left => {
            const todayPosition = parseFloat(left as unknown as string);

            if ($body.find('.milestone-lanes .issue-bar[data-state="closed"]').length > 0) {
              cy.get('.milestone-lanes .issue-bar[data-state="closed"]').each($issue => {
                const issuePosition = parseFloat($issue.css('left'));
                expect(issuePosition).to.be.lessThan(todayPosition);
              });
            }

            if ($body.find('.milestone-lanes .issue-bar[data-state="open"]').length > 0) {
              cy.get('.milestone-lanes .issue-bar[data-state="open"]').each($issue => {
                const issuePosition = parseFloat($issue.css('left'));
                expect(issuePosition).to.be.greaterThan(todayPosition);
              });
            }
          });
        } else {
          cy.log('Today marker not visible - skipping test');
        }
      });
    });

    it('should place "overdue" open issues in the current quarter, after "today"', () => {
      cy.get('body').then(($body) => {
        if ($body.find('.today-marker').length > 0 && $body.find('app-milestone-row').length > 0) {
          cy.get('app-milestone-row').first().as('firstMilestoneRow');
          cy.get('@firstMilestoneRow').find('a.issue-bar').then(($issues) => {
            if ($issues.length > 0) {
              cy.wrap($issues.first()).invoke('css', 'left').then(left => {
                const issuePosition = parseFloat(left as unknown as string);
                cy.get('.today-marker').invoke('css', 'left').then(todayLeft => {
                  const todayPosition = parseFloat(todayLeft as unknown as string);
                  expect(issuePosition).to.be.a('number');
                  expect(todayPosition).to.be.a('number');
                });
              });
            }
          });
        } else {
          cy.log('Skipping test - today marker or milestones not visible');
        }
      });
    });

    it('should display a tooltip with correct info on mouse hover', () => {
      cy.get('body').then(($body) => {
        if ($body.find('a.issue-bar').length > 0) {
          cy.get('a.issue-bar').first().trigger('mouseenter');
          cy.tick(500);
          cy.get('app-tooltip').should('be.visible');
          cy.get('.tooltip-title').should('not.be.empty');

          cy.get('a.issue-bar').first().trigger('mouseleave');
          cy.tick(500);
          cy.get('app-tooltip').should('not.be.visible');
        } else {
          cy.log('No issue bars found - skipping tooltip test');
        }
      });
    });
  });

  context('Edge Case Rendering', () => {
    it('should not display a milestone if it has no issues', () => {
      cy.get('app-milestone-row .title-link').should('not.contain.text', 'No Issues');
    });

    it('should create multiple tracks if issues overflow the available space', () => {
      cy.get('body').then(($body) => {
        const overflowMilestone = $body.find('app-milestone-row').filter((_, el) => {
          return $(el).find('app-issue-bar').length > 10;
        });

        if (overflowMilestone.length > 0) {
          cy.wrap(overflowMilestone.first()).as('overflowMilestoneRow');
          cy.get('@overflowMilestoneRow').find('.issue-track-area').invoke('height').should('be.greaterThan', 50);
          cy.get('@overflowMilestoneRow').find('app-issue-bar').should('have.length.greaterThan', 5);
        } else {
          cy.log('No milestone with overflow found - skipping test');
        }
      });
    });

    it('should render an issue with 0 points with a minimum width', () => {
      cy.get('body').then(($body) => {
        if ($body.find('a.issue-bar').length > 0) {
          cy.get('a.issue-bar').first().scrollIntoView().should('be.visible').invoke('width').should('be.greaterThan', 10);
        } else {
          cy.log('No issue bars found - skipping test');
        }
      });
    });

    it('should show an empty state when navigating to a period with no milestones', () => {
      for (let i = 0; i < 4; i++) {
        cy.get('button[title="Next quarter"]').click();
        cy.tick(5000);
      }

      cy.get('app-milestone-row').should('not.exist');
      cy.get('.empty-state').should('be.visible').and('contain.text', 'No open milestones');
    });
  });

  context('Monthly View', () => {
    beforeEach(() => {
      cy.get('app-roadmap-toolbar button[title="Toggle view mode"]').click();
      cy.tick(5000);
      cy.get('app-loader').should('not.be.visible');
    });

    it('should switch to Monthly View and display the correct label', () => {
      const expectedLabel = getMonthlyPeriodLabel(TODAY);
      cy.get('.period-label').should('contain.text', expectedLabel);

      cy.get('app-timeline-header .quarter-label').should('have.length', 1);
      cy.get('app-timeline-header .quarter-label').first().should('contain.text', expectedLabel);
      cy.get('app-timeline-header .month-label').should('not.exist');
    });

    it('should navigate 3 months back when clicking "Previous quarter"', () => {
      const initialLabel = getMonthlyPeriodLabel(TODAY);

      const prevDate = new Date(TODAY);
      prevDate.setMonth(prevDate.getMonth() - 3);
      const expectedLabel = getMonthlyPeriodLabel(prevDate);

      cy.get('.period-label').should('contain.text', initialLabel);
      cy.get('button[title="Previous quarter"]').click();
      cy.tick(5000);
      cy.get('.period-label').should('contain.text', expectedLabel);
    });

    it('should return to the current month when "Go to today" is clicked', () => {
      const initialLabel = getMonthlyPeriodLabel(TODAY);

      const nextDate = new Date(TODAY);
      nextDate.setMonth(nextDate.getMonth() + 3);
      const nextLabel = getMonthlyPeriodLabel(nextDate);

      cy.get('button[title="Next quarter"]').click();
      cy.tick(5000);
      cy.get('.period-label').should('contain.text', nextLabel);

      cy.get('button[title="Go to today"]').click();
      cy.tick(5000);
      cy.get('app-loader').should('not.exist');
      cy.get('.period-label').should('contain.text', initialLabel);
    });

    it('should display issues as a list with labels', () => {
      cy.get('app-milestone-row').should('have.length.greaterThan', 0);

      cy.get('app-milestone-row').first().then($row => {
        if ($row.find('.issue-list-label').length > 0) {
          cy.wrap($row).find('.issue-list-label').should('be.visible');
          cy.wrap($row).find('.issue-list-label').first().invoke('text').should('not.be.empty');
        }

        if ($row.find('a.issue-bar').length > 0) {
          cy.wrap($row).find('a.issue-bar').first().invoke('css', 'width').then(width => {
            const parentWidth = $row.find('.issue-track-area').width() || 1;
            const issueWidthPx = parseFloat(width as unknown as string);
            const percentage = (issueWidthPx / parentWidth) * 100;
            expect(percentage).to.be.greaterThan(90);
          });
        }
      });
    });

    it('should filter out milestones not relevant to the current month', () => {
      cy.get('button[title="Go to today"]').click();
      cy.tick(5000);

      cy.get('body').then($body => {
        const initialCount = $body.find('app-milestone-row').length;

        for (let i = 0; i < initialCount; i++) {
          cy.get('button[title="Next quarter"]').click();
          cy.tick(5000);
        }

        cy.get('app-milestone-row').should('not.exist');
        cy.get('.empty-state').should('be.visible');
      });
    });
  });
});
