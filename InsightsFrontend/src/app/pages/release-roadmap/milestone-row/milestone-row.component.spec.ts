import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatePipe } from '@angular/common';
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
  { name: 'Q3 2025', monthCount: 3 },
  { name: 'Q4 2025', monthCount: 3 },
];

const MOCK_MILESTONE: Milestone = {
  id: 'm1',
  title: 'Test Milestone 9.3.0',
  openIssueCount: 1,
  closedIssueCount: 1,
  url: 'http://example.com/milestone/1',
  dueOn: new Date('2025-09-30T00:00:00.000Z'),
  isEstimated: false,
  number: 1,
  state: GitHubStates.OPEN,
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
  closedAt: new Date('2025-07-20T00:00:00.000Z'),
};

describe('MilestoneRowComponent', () => {
  let component: MilestoneRowComponent;
  let fixture: ComponentFixture<MilestoneRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MilestoneRowComponent, IssueBarComponent, NoopAnimationsModule],
      providers: [DatePipe],
    }).compileComponents();

    fixture = TestBed.createComponent(MilestoneRowComponent);
    component = fixture.componentInstance;

    jasmine.clock().install();
    jasmine.clock().mockDate(MOCK_TODAY);

    component.timelineStartDate = MOCK_TIMELINE_START;
    component.timelineEndDate = MOCK_TIMELINE_END;
    component.totalTimelineDays = MOCK_TOTAL_DAYS;
    component.quarters = MOCK_QUARTERS;
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should create', () => {
    component.milestone = MOCK_MILESTONE;
    component.issues = [];
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should calculate progress as 50% for 1 open and 1 closed issue', () => {
    component.milestone = { ...MOCK_MILESTONE, openIssueCount: 1, closedIssueCount: 1 };
    fixture.detectChanges();

    expect(component.progressPercentage).toBe(50);
  });

  describe('Layout Algorithm Scenarios', () => {
    it('should place closed issues before "today" and open issues after "today" in the CURRENT quarter', () => {
      component.milestone = MOCK_MILESTONE;
      component.issues = [MOCK_OPEN_ISSUE, MOCK_CLOSED_ISSUE];
      fixture.detectChanges();

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
      component.milestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-12-15T00:00:00.000Z') };
      component.issues = [MOCK_OPEN_ISSUE];
      fixture.detectChanges();

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];

      const q4StartDays = (new Date('2025-10-01').getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(q4StartDays);
    });

    it('should place all issues in the "closed" window for a PAST quarter', () => {
      const pastMilestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-06-15T00:00:00.000Z') };
      const pastClosedIssue = { ...MOCK_CLOSED_ISSUE, closedAt: new Date('2025-05-20T00:00:00.000Z') };

      component.milestone = pastMilestone;
      component.issues = [pastClosedIssue];
      component.quarters = [{ name: 'Q2 2025', monthCount: 3 }, ...MOCK_QUARTERS];
      fixture.detectChanges();

      expect(component.positionedIssues.length).toBe(1);
    });

    it('should move "overdue" open issues to the current quarter for layout', () => {
      component.milestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-06-15T00:00:00.000Z') };
      component.issues = [MOCK_OPEN_ISSUE];
      fixture.detectChanges();

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];
      const todayStartDays = (MOCK_TODAY.getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(todayStartDays);
    });

    it('should only render issues that fall into the visible quarters provided by input', () => {
      const futureIssue = { ...MOCK_OPEN_ISSUE, id: 'future-open' };
      component.milestone = { ...MOCK_MILESTONE, dueOn: new Date('2025-11-15T00:00:00.000Z') };
      component.issues = [futureIssue];
      component.quarters = [{ name: 'Q3 2025', monthCount: 3 }];
      fixture.detectChanges();

      expect(component.positionedIssues.length).toBe(0);
    });

    it('should create multiple tracks if issues overflow a planning window', () => {
      const largeIssues = Array.from({ length: 10 }, (_, index) => ({
        ...MOCK_OPEN_ISSUE,
        id: `issue-${index}`,
        points: 20,
      }));
      component.milestone = MOCK_MILESTONE;
      component.issues = largeIssues;
      fixture.detectChanges();

      expect(component.trackCount).toBeGreaterThan(1);
    });

    it('should sort issues by priority (critical first) before layout', () => {
      const lowPrio: Issue = { ...MOCK_OPEN_ISSUE, id: 'low', issuePriority: { name: 'Low' } as IssuePriority };
      const critPrio: Issue = { ...MOCK_OPEN_ISSUE, id: 'crit', issuePriority: { name: 'Critical' } as IssuePriority };

      component.milestone = MOCK_MILESTONE;
      component.issues = [lowPrio, critPrio];
      component.trackCount = 1;
      fixture.detectChanges();

      const critIndex = component.positionedIssues.findIndex((p) => p.issue.id === 'crit');
      const lowIndex = component.positionedIssues.findIndex((p) => p.issue.id === 'low');

      expect(critIndex).toBeLessThan(lowIndex);
    });
  });
});
