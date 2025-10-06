import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatePipe } from '@angular/common';
import { SimpleChanges } from '@angular/core';
import { MilestoneRowComponent } from './milestone-row.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue, IssuePriority } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

const MOCK_TIMELINE_START = new Date('2025-07-01T00:00:00.000Z');
const MOCK_TIMELINE_END = new Date('2025-12-31T23:59:59.999Z');
const MOCK_TOTAL_DAYS = 184;
const MOCK_TODAY = new Date('2025-08-15T12:00:00.000Z');

const MOCK_QUARTERS = [
  { name: 'Q2 2025', monthCount: 3 },
  { name: 'Q3 2025', monthCount: 3 },
  { name: 'Q4 2025', monthCount: 3 },
];

const MOCK_MILESTONE: Milestone = {
  id: 'm1',
  number: 1,
  title: 'Test Milestone 9.3.0',
  url: 'http://example.com/milestone/1',
  state: GitHubStates.OPEN,
  dueOn: new Date('2025-09-30T00:00:00.000Z'),
  openIssueCount: 1,
  closedIssueCount: 1,
  isEstimated: false,
};

const MOCK_OPEN_ISSUE: Issue = {
  id: 'issue-open',
  number: 101,
  title: 'Open Issue',
  state: GitHubStates.OPEN,
  points: 10,
  url: 'http://example.com/issue/101',
};

const MOCK_CLOSED_ISSUE: Issue = {
  id: 'issue-closed',
  number: 102,
  title: 'Closed Issue',
  state: GitHubStates.CLOSED,
  points: 5,
  url: 'http://example.com/issue/102',
  closedAt: new Date('2025-07-20T00:00:00.000Z'), // Closed in Q3, before today
};

describe('MilestoneRowComponent', () => {
  let component: MilestoneRowComponent;
  let fixture: ComponentFixture<MilestoneRowComponent>;

  // Helper function to initialize component with inputs and trigger ngOnChanges
  const initializeComponent = (milestone: Milestone, issues: Issue[]) => {
    component.milestone = milestone;
    component.issues = issues;
    // Manually call ngOnChanges to simulate the parent component's behavior
    const changes: SimpleChanges = {
      milestone: {
        currentValue: component.milestone,
        previousValue: null,
        isFirstChange: () => true,
        firstChange: true,
      },
      issues: { currentValue: component.issues, previousValue: null, isFirstChange: () => true, firstChange: true },
      quarters: { currentValue: component.quarters, previousValue: null, isFirstChange: () => true, firstChange: true },
    };
    component.ngOnChanges(changes);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MilestoneRowComponent, IssueBarComponent, NoopAnimationsModule],
      providers: [DatePipe],
    }).compileComponents();

    fixture = TestBed.createComponent(MilestoneRowComponent);
    component = fixture.componentInstance;

    jasmine.clock().install();
    jasmine.clock().mockDate(MOCK_TODAY);

    // Set timeline properties that don't change per test
    component.timelineStartDate = MOCK_TIMELINE_START;
    component.timelineEndDate = MOCK_TIMELINE_END;
    component.totalTimelineDays = MOCK_TOTAL_DAYS;
    component.quarters = MOCK_QUARTERS;
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should create', () => {
    initializeComponent(MOCK_MILESTONE, []);

    expect(component).toBeTruthy();
  });

  it('should calculate progress as 50% for 1 open and 1 closed issue', () => {
    const milestone: Milestone = { ...MOCK_MILESTONE, openIssueCount: 1, closedIssueCount: 1 };
    initializeComponent(milestone, []);

    expect(component.progressPercentage).toBe(50);
  });

  describe('Layout Algorithm Scenarios', () => {
    it('should place closed issues before "today" and open issues after "today" in the CURRENT quarter', () => {
      initializeComponent(MOCK_MILESTONE, [MOCK_OPEN_ISSUE, MOCK_CLOSED_ISSUE]);

      const closedPositioned = component.positionedIssues.find((p) => p.issue.id === 'issue-closed');
      const openPositioned = component.positionedIssues.find((p) => p.issue.id === 'issue-open');

      expect(closedPositioned).withContext('Closed issue should be positioned').toBeDefined();
      expect(openPositioned).withContext('Open issue should be positioned').toBeDefined();

      const closedLeft = Number.parseFloat(closedPositioned!.style['left']!);
      const openLeft = Number.parseFloat(openPositioned!.style['left']!);

      expect(closedLeft).toBeLessThan(openLeft);
      expect(component.trackCount).toBe(1);
    });

    it('should place all issues in the "open" window for a FUTURE quarter', () => {
      const futureMilestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-12-15T00:00:00.000Z') }; // Q4
      initializeComponent(futureMilestone, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];

      const q4StartDays = (new Date('2025-10-01').getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(q4StartDays);
    });

    it('should place all issues in the "closed" window for a PAST quarter', () => {
      const pastMilestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-06-15T00:00:00.000Z') }; // Q2
      const pastClosedIssue = { ...MOCK_CLOSED_ISSUE, closedAt: new Date('2025-05-20T00:00:00.000Z') }; // Closed in Q2
      initializeComponent(pastMilestone, [pastClosedIssue]);

      expect(component.positionedIssues.length).toBe(1);
    });

    it('should move "overdue" open issues to the current quarter for layout', () => {
      const overdueMilestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-06-15T00:00:00.000Z') }; // Due in Q2
      initializeComponent(overdueMilestone, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];
      const todayStartDays = (MOCK_TODAY.getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(todayStartDays);
    });

    it('should only render issues that fall into the visible quarters provided by input', () => {
      const futureMilestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-11-15T00:00:00.000Z') }; // Q4
      component.quarters = [{ name: 'Q3 2025', monthCount: 3 }];
      initializeComponent(futureMilestone, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(0);
    });

    it('should create multiple tracks if issues overflow a planning window', () => {
      const largeIssues = Array.from({ length: 10 }, (_, index) => ({
        ...MOCK_OPEN_ISSUE,
        id: `issue-${index}`,
        points: 20,
      }));
      initializeComponent(MOCK_MILESTONE, largeIssues);

      expect(component.trackCount).toBeGreaterThan(1);
    });
  });
});
