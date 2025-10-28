import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatePipe } from '@angular/common';
import { SimpleChanges } from '@angular/core';
import { MilestoneRowComponent } from './milestone-row.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReleaseRoadmapComponent } from '../release-roadmap.component';

const MOCK_TIMELINE_START = new Date(2025, 6, 1);
const MOCK_TIMELINE_END = new Date(2025, 11, 31, 23, 59, 59, 999);
const MOCK_TOTAL_DAYS = 184;
const MOCK_TODAY = new Date(2025, 7, 15, 12, 0, 0); // Aug 15

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
  dueOn: new Date(2025, 8, 30), // Sep 30
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
  closedAt: new Date(2025, 6, 20),
};

describe('MilestoneRowComponent', () => {
  let component: MilestoneRowComponent;
  let fixture: ComponentFixture<MilestoneRowComponent>;
  let releaseRoadmapComponentSpy: jasmine.SpyObj<ReleaseRoadmapComponent>;

  const initializeComponent = (milestone: Milestone, issues: Issue[]) => {
    component.milestone = milestone;
    component.issues = issues;
    const changes: SimpleChanges = {
      milestone: {
        currentValue: component.milestone,
        previousValue: null,
        isFirstChange: () => true,
        firstChange: true,
      },
      issues: { currentValue: component.issues, previousValue: null, isFirstChange: () => true, firstChange: true },
      quarters: { currentValue: component.quarters, previousValue: null, isFirstChange: () => true, firstChange: true },
      viewMode: { currentValue: component.viewMode, previousValue: null, isFirstChange: () => true, firstChange: true },
    };
    component.ngOnChanges(changes);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    releaseRoadmapComponentSpy = jasmine.createSpyObj('ReleaseRoadmapComponent', [
      'createQuarterWindow',
      'getQuarterFromDate',
      'getQuarterKey',
    ]);

    releaseRoadmapComponentSpy.getQuarterFromDate.and.callFake((date: Date) => {
      const year = date.getFullYear();
      const quarterIndex = Math.floor(date.getMonth() / 3);
      return new Date(year, quarterIndex * 3, 1);
    });

    releaseRoadmapComponentSpy.getQuarterKey.and.callFake((quarter: Date) => {
      const year = quarter.getFullYear();
      const quarterNumber = Math.floor(quarter.getMonth() / 3) + 1;
      return `Q${quarterNumber} ${year}`;
    });

    releaseRoadmapComponentSpy.createQuarterWindow.and.callFake((quarterMatch: RegExpMatchArray) => {
      const quarterNumber = Number.parseInt(quarterMatch[1], 10);
      const year = Number.parseInt(quarterMatch[2], 10);
      const startDate = new Date(year, (quarterNumber - 1) * 3, 1);
      const endDate = new Date(year, quarterNumber * 3, 0);
      endDate.setHours(23, 59, 59, 999);
      return { start: startDate.getTime(), end: endDate.getTime() };
    });

    await TestBed.configureTestingModule({
      imports: [MilestoneRowComponent, IssueBarComponent, NoopAnimationsModule],
      providers: [
        DatePipe,
        { provide: ReleaseRoadmapComponent, useValue: releaseRoadmapComponentSpy },
      ],
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

      const closedPositioned = component.positionedIssues.find((p) => p.issue!.id === 'issue-closed');
      const openPositioned = component.positionedIssues.find((p) => p.issue!.id === 'issue-open');

      expect(closedPositioned).withContext('Closed issue should be positioned').toBeDefined();
      expect(openPositioned).withContext('Open issue should be positioned').toBeDefined();

      const closedLeft = Number.parseFloat(closedPositioned!.style['left']!);
      const openLeft = Number.parseFloat(openPositioned!.style['left']!);

      expect(closedLeft).toBeLessThan(openLeft);
      expect(component.trackCount).toBe(1);
    });

    it('should place all issues in the "open" window for a FUTURE quarter', () => {
      const futureMilestone = { ...MOCK_MILESTONE, dueOn: new Date(2025, 11, 15) };
      initializeComponent(futureMilestone, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];

      const q4StartDays = (new Date(2025, 9, 1).getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(q4StartDays);
    });

    it('should place all issues in the "closed" window for a PAST quarter', () => {
      const pastMilestone = { ...MOCK_MILESTONE, dueOn: new Date(2025, 5, 15) };
      const pastClosedIssue = { ...MOCK_CLOSED_ISSUE, closedAt: new Date(2025, 4, 20) };
      initializeComponent(pastMilestone, [pastClosedIssue]);

      expect(component.positionedIssues.length).toBe(1);
    });

    it('should move "overdue" open issues to the current quarter for layout', () => {
      const overdueMilestone = { ...MOCK_MILESTONE, dueOn: new Date(2025, 5, 15) };
      initializeComponent(overdueMilestone, [MOCK_OPEN_ISSUE]);

      expect(component.positionedIssues.length).toBe(1);
      const issuePos = component.positionedIssues[0];
      const todayStartDays = (MOCK_TODAY.getTime() - MOCK_TIMELINE_START.getTime()) / (1000 * 3600 * 24);
      const issueStartDays = (Number.parseFloat(issuePos.style['left']!) / 100) * MOCK_TOTAL_DAYS;

      expect(issueStartDays).toBeGreaterThanOrEqual(todayStartDays);
    });

    it('should only render issues that fall into the visible quarters provided by input', () => {
      const futureMilestone = { ...MOCK_MILESTONE, dueOn: new Date(2025, 10, 15) };
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

  describe('Unplanned Epics Layout', () => {
    const UNPLANNED_MILESTONE: Milestone = {
      id: 'unplanned-epics',
      number: 0,
      title: 'Unplanned Epics',
      url: '',
      state: GitHubStates.OPEN,
      dueOn: new Date(2025, 9, 1),
      openIssueCount: 3,
      closedIssueCount: 0,
      isEstimated: false,
    };

    const MOCK_EPIC_ISSUES: Issue[] = [
      {
        id: 'epic1',
        number: 100,
        title: 'Epic One',
        state: GitHubStates.OPEN,
        url: 'http://example.com/epic1',
        issueType: { id: 'epic-type', name: 'Epic', description: 'Epic type', color: 'purple' },
      } as Issue,
      {
        id: 'epic2',
        number: 200,
        title: 'Epic Two',
        state: GitHubStates.OPEN,
        url: 'http://example.com/epic2',
        issueType: { id: 'epic-type', name: 'Epic', description: 'Epic type', color: 'purple' },
      } as Issue,
      {
        id: 'epic3',
        number: 300,
        title: 'Epic Three',
        state: GitHubStates.OPEN,
        url: 'http://example.com/epic3',
        issueType: { id: 'epic-type', name: 'Epic', description: 'Epic type', color: 'purple' },
      } as Issue,
    ];

    it('should use single lane layout for unplanned epics', () => {
      initializeComponent(UNPLANNED_MILESTONE, MOCK_EPIC_ISSUES);

      expect(component.trackCount).toBe(1);
    });

    it('should layout all epics in a row with 30 day width', () => {
      initializeComponent(UNPLANNED_MILESTONE, MOCK_EPIC_ISSUES);

      expect(component.positionedIssues.length).toBe(3);
      expect(component.positionedIssues[0].track).toBe(0);
      expect(component.positionedIssues[1].track).toBe(0);
      expect(component.positionedIssues[2].track).toBe(0);
    });

    it('should position epics sequentially with gaps', () => {
      initializeComponent(UNPLANNED_MILESTONE, MOCK_EPIC_ISSUES);

      const epic1 = component.positionedIssues[0];
      const epic2 = component.positionedIssues[1];
      const epic3 = component.positionedIssues[2];

      expect(epic1.issue!.id).toBe('epic1');
      expect(epic2.issue!.id).toBe('epic2');
      expect(epic3.issue!.id).toBe('epic3');

      expect(epic1.style['left']).toBeDefined();
      expect(epic1.style['width']).toBeDefined();
      expect(epic2.style['left']).toBeDefined();
      expect(epic2.style['width']).toBeDefined();
    });

    it('should stop adding epics when timeline end is reached', () => {
      const manyEpics = Array.from({ length: 20 }, (_, index) => ({
        id: `epic-${index}`,
        number: (index + 1) * 100,
        title: `Epic ${index + 1}`,
        state: GitHubStates.OPEN,
        url: `http://example.com/epic${index}`,
        issueType: { id: 'epic-type', name: 'Epic', description: 'Epic type', color: 'purple' },
      })) as Issue[];

      initializeComponent(UNPLANNED_MILESTONE, manyEpics);

      expect(component.positionedIssues.length).toBeLessThan(20);
      expect(component.positionedIssues.length).toBeGreaterThan(0);
    });

    it('should maintain track count of 1 even with many epics', () => {
      const manyEpics = Array.from({ length: 10 }, (_, index) => ({
        id: `epic-${index}`,
        number: (index + 1) * 100,
        title: `Epic ${index + 1}`,
        state: GitHubStates.OPEN,
        url: `http://example.com/epic${index}`,
        issueType: { id: 'epic-type', name: 'Epic', description: 'Epic type', color: 'purple' },
      })) as Issue[];

      initializeComponent(UNPLANNED_MILESTONE, manyEpics);

      expect(component.trackCount).toBe(1);
    });
  });
});
