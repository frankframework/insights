import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DatePipe } from '@angular/common';
import { MilestoneRowComponent } from './milestone-row.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';

const MOCK_START_DATE = new Date('2025-04-01T00:00:00.000Z');
const MOCK_TOTAL_DAYS = 182;
const MOCK_TODAY = new Date('2025-05-15T00:00:00.000Z');

const MOCK_CURRENT_QUARTER_MILESTONE: Milestone = {
  id: 'milestone-1',
  title: 'Test Milestone 9.2.0 (Current Quarter)',
  openIssueCount: 1,
  closedIssueCount: 1,
  url: 'http://example.com/milestone/1',
  dueOn: new Date('2025-06-30T00:00:00.000Z'),
  isEstimated: true,
  number: 1,
  state: GitHubStates.OPEN,
  major: 9,
  minor: 2,
  patch: 0,
};

const MOCK_FUTURE_QUARTER_MILESTONE: Milestone = {
  ...MOCK_CURRENT_QUARTER_MILESTONE,
  id: 'milestone-3',
  title: 'Test Milestone 9.3.0 (Future Quarter)',
  dueOn: new Date('2025-09-30T00:00:00.000Z'),
};

const MOCK_OPEN_ISSUE: Issue = {
  id: 'issue-1',
  number: 101,
  title: 'Open Issue',
  state: GitHubStates.OPEN,
  points: 5,
  url: 'http://example.com/issue/101',
};

const MOCK_CLOSED_ISSUE: Issue = {
  id: 'issue-2',
  number: 102,
  title: 'Closed Issue',
  state: GitHubStates.CLOSED,
  points: 3,
  url: 'http://example.com/issue/102',
};

describe('MilestoneRowComponent', () => {
  let component: MilestoneRowComponent;
  let fixture: ComponentFixture<MilestoneRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MilestoneRowComponent, IssueBarComponent],
      providers: [DatePipe],
    }).compileComponents();

    fixture = TestBed.createComponent(MilestoneRowComponent);
    component = fixture.componentInstance;

    jasmine.clock().install();
    jasmine.clock().mockDate(MOCK_TODAY);
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  const initializeComponent = (milestone: Milestone, issues: Issue[]): void => {
    component.milestone = milestone;
    component.issues = issues;
    component.timelineStartDate = MOCK_START_DATE;
    component.totalTimelineDays = MOCK_TOTAL_DAYS;
    fixture.detectChanges();
  };

  it('should create', () => {
    initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, dueOn: null }, []);

    expect(component).toBeTruthy();
  });

  describe('Progress Calculation', () => {
    it('should calculate progress as 50% for 1 open and 1 closed issue', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, openIssueCount: 1, closedIssueCount: 1 }, []);

      expect(component.progressPercentage).toBe(50);
    });

    it('should calculate progress as 100% when all issues are closed', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, openIssueCount: 0, closedIssueCount: 5 }, []);

      expect(component.progressPercentage).toBe(100);
    });

    it('should calculate progress as 0% for no issues', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, openIssueCount: 0, closedIssueCount: 0 }, []);

      expect(component.progressPercentage).toBe(0);
    });
  });

  describe('Layout Algorithm Logic', () => {
    it('should not run layout algorithm if dueOn is not set', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, dueOn: null }, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(0);
    });

    it('should place a single issue on the first track', () => {
      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(1);
      expect(component.trackCount).toBe(1);
    });

    it('should create multiple tracks if issues require more space than available', () => {
      const largeIssues = Array.from({ length: 10 }, (_, index) => ({
        ...MOCK_OPEN_ISSUE,
        id: `issue-${index}`,
        points: 30,
      }));
      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, largeIssues);

      expect(component.trackCount).toBeGreaterThan(1);
    });

    it('should sort issues by priority before placing them', () => {
      const lowPrio: Issue = {
        ...MOCK_OPEN_ISSUE,
        id: 'low',
        issuePriority: { name: 'Low', id: 'p4', color: '', description: '' },
      };
      const criticalPrio: Issue = {
        ...MOCK_OPEN_ISSUE,
        id: 'crit',
        issuePriority: { name: 'Critical', id: 'p1', color: '', description: '' },
      };

      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, [lowPrio, criticalPrio]);

      expect(component.positionedIssues[0].issue.id).toBe('crit');
    });

    describe('For Current Quarter Milestones', () => {
      it('should place closed issues before "today" and open issues after "today"', () => {
        initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, [MOCK_OPEN_ISSUE, MOCK_CLOSED_ISSUE]);

        const closedPositioned = component.positionedIssues.find((p) => p.issue.id === MOCK_CLOSED_ISSUE.id);
        const openPositioned = component.positionedIssues.find((p) => p.issue.id === MOCK_OPEN_ISSUE.id);

        const closedLeft = Number.parseFloat(closedPositioned!.style['left']!);
        const openLeft = Number.parseFloat(openPositioned!.style['left']!);

        expect(closedLeft).toBeLessThan(openLeft);
      });
    });

    describe('For Past/Future Quarter Milestones', () => {
      it('should layout closed issues before open issues sequentially', () => {
        initializeComponent(MOCK_FUTURE_QUARTER_MILESTONE, [MOCK_OPEN_ISSUE, MOCK_CLOSED_ISSUE]);

        const closedPositioned = component.positionedIssues.find((p) => p.issue.id === MOCK_CLOSED_ISSUE.id)!;
        const openPositioned = component.positionedIssues.find((p) => p.issue.id === MOCK_OPEN_ISSUE.id)!;

        expect(closedPositioned.style['left']).toBeDefined();
        expect(openPositioned.style['left']).toBeDefined();
        expect(closedPositioned.style['width']).toBeDefined();

        const closedLeft = Number.parseFloat(closedPositioned.style['left']!);
        const openLeft = Number.parseFloat(openPositioned.style['left']!);
        const closedWidth = Number.parseFloat(closedPositioned.style['width']!);

        expect(closedLeft).toBeLessThan(openLeft);

        expect(closedLeft + closedWidth).toBeLessThanOrEqual(openLeft + 0.01);
      });
    });
  });

  describe('Template Rendering', () => {
    let nativeElement: HTMLElement;

    beforeEach(() => {
      nativeElement = fixture.nativeElement;
    });

    it('should display the milestone title and link', () => {
      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, []);
      const link = nativeElement.querySelector('.title-link') as HTMLAnchorElement;

      expect(link.textContent).toContain(MOCK_CURRENT_QUARTER_MILESTONE.title);
      expect(link.href).toBe(MOCK_CURRENT_QUARTER_MILESTONE.url);
    });

    it('should display the formatted due date', () => {
      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, []);
      const detail = nativeElement.querySelector('.detail');

      expect(detail?.textContent).toContain('30-06-2025');
    });

    it('should display "Unplanned" when due date is null', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, dueOn: null }, []);
      const detail = nativeElement.querySelector('.detail.unplanned');

      expect(detail).toBeTruthy();
      expect(detail?.textContent).toContain('Unplanned');
    });

    it('should show the estimated icon when isEstimated is true', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, isEstimated: true }, []);
      const icon = nativeElement.querySelector('.estimated-icon');

      expect(icon).toBeTruthy();
    });

    it('should NOT show the estimated icon when isEstimated is false', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, isEstimated: false }, []);
      const icon = nativeElement.querySelector('.estimated-icon');

      expect(icon).toBeFalsy();
    });

    it('should set the progress bar width style correctly', () => {
      initializeComponent({ ...MOCK_CURRENT_QUARTER_MILESTONE, openIssueCount: 1, closedIssueCount: 3 }, []);
      const progressBar = nativeElement.querySelector('.progress-bar') as HTMLElement;

      expect(progressBar.style.width).toBe('75%');
    });

    it('should render the correct number of issue-bar components', () => {
      initializeComponent(MOCK_CURRENT_QUARTER_MILESTONE, [MOCK_OPEN_ISSUE, MOCK_CLOSED_ISSUE]);
      const issueBars = fixture.debugElement.queryAll(By.css('app-issue-bar'));

      expect(issueBars.length).toBe(2);
    });
  });
});
