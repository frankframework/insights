import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReleaseRoadmapComponent } from './release-roadmap.component';
import { MilestoneService, Milestone } from '../../services/milestone.service';
import { IssueService, Issue } from '../../services/issue.service';
import { GitHubStates } from '../../app.service';

const MOCK_MILESTONES: Milestone[] = [
  {
    id: 'm1',
    number: 1,
    title: 'Release 9.2.0 (Visible)',
    url: 'http://example.com/m1',
    state: GitHubStates.OPEN,
    dueOn: new Date('2025-08-15T00:00:00.000Z'),
    openIssueCount: 1,
    closedIssueCount: 1,
    isEstimated: false,
  },
  {
    id: 'm2',
    number: 2,
    title: 'Release 9.1.0 (Overdue, becomes visible)',
    url: 'http://example.com/m2',
    state: GitHubStates.OPEN,
    dueOn: new Date('2025-03-15T00:00:00.000Z'),
    openIssueCount: 1,
    closedIssueCount: 0,
    isEstimated: false,
  },
  {
    id: 'm3',
    number: 3,
    title: 'Release 9.3.0 (Not visible initially)',
    url: 'http://example.com/m3',
    state: GitHubStates.OPEN,
    dueOn: new Date('2025-12-15T00:00:00.000Z'),
    openIssueCount: 1,
    closedIssueCount: 0,
    isEstimated: false,
  },
];

const MOCK_ISSUES_M1: Issue[] = [
  {
    id: 'i1-closed',
    number: 10,
    title: 'Closed issue in Q3',
    state: GitHubStates.CLOSED,
    url: '',
    closedAt: new Date('2025-07-20T00:00:00.000Z'),
  } as Issue,
];
const MOCK_ISSUES_M2: Issue[] = [
  { id: 'i2-open', number: 20, title: 'Overdue open issue', state: GitHubStates.OPEN, url: '' } as Issue,
];
const MOCK_ISSUES_M3: Issue[] = [
  { id: 'i3-open', number: 30, title: 'Future open issue', state: GitHubStates.OPEN, url: '' } as Issue,
];

describe('ReleaseRoadmapComponent', () => {
  let component: ReleaseRoadmapComponent;
  let fixture: ComponentFixture<ReleaseRoadmapComponent>;
  let milestoneService: jasmine.SpyObj<MilestoneService>;
  let issueService: jasmine.SpyObj<IssueService>;
  let toastrService: jasmine.SpyObj<ToastrService>;

  beforeEach(async () => {
    const milestoneSpy = jasmine.createSpyObj('MilestoneService', ['getOpenMilestones']);
    const issueSpy = jasmine.createSpyObj('IssueService', ['getIssuesByMilestoneId']);
    const toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);

    await TestBed.configureTestingModule({
      imports: [ReleaseRoadmapComponent, NoopAnimationsModule],
      providers: [
        { provide: MilestoneService, useValue: milestoneSpy },
        { provide: IssueService, useValue: issueSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseRoadmapComponent);
    component = fixture.componentInstance;
    milestoneService = TestBed.inject(MilestoneService) as jasmine.SpyObj<MilestoneService>;
    issueService = TestBed.inject(IssueService) as jasmine.SpyObj<IssueService>;
    toastrService = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;

    milestoneService.getOpenMilestones.and.returnValue(of(MOCK_MILESTONES));
    issueService.getIssuesByMilestoneId.and.callFake((id: string) => {
      if (id === 'm1') return of(MOCK_ISSUES_M1);
      if (id === 'm2') return of(MOCK_ISSUES_M2);
      if (id === 'm3') return of(MOCK_ISSUES_M3);
      return of([]);
    });
  });

  it('should create', () => {
    milestoneService.getOpenMilestones.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Timeline Period and Filtering Logic', () => {
    beforeEach(() => {
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date('2025-07-10T12:00:00.000Z'));
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('should set displayDate to the start of the current quarter (Q3 2025) on resetPeriod', fakeAsync(() => {
      component.resetPeriod();
      tick();

      expect(component.displayDate.getFullYear()).toBe(2025);
      expect(component.displayDate.getMonth()).toBe(6);
      expect(component.quarters[0].name).toBe('Q3 2025');
      expect(component.quarters[1].name).toBe('Q4 2025');
    }));

    it('should only show milestones that have issues within the visible quarters (Q3 & Q4)', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(component.openMilestones.length).toBe(3);
      const visibleIds = component.openMilestones.map((m) => m.id);

      expect(visibleIds).toContain('m1');
      expect(visibleIds).toContain('m2');
      expect(visibleIds).toContain('m3');
    }));

    it('should filter out milestones when their issues are not in the new view (Q2 & Q3)', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(component.openMilestones.length).toBe(3);

      component.changePeriod(-3);
      tick();

      expect(component.openMilestones.length).toBe(2);
      const visibleIds = component.openMilestones.map((m) => m.id);

      expect(visibleIds).toContain('m1');
      expect(visibleIds).toContain('m2');
      expect(visibleIds).not.toContain('m3');
    }));

    it('should handle errors during data loading and show a toastr message', fakeAsync(() => {
      milestoneService.getOpenMilestones.and.returnValue(throwError(() => new Error('API Error')));

      fixture.detectChanges();
      tick();

      expect(component.isLoading).toBeFalse();
      expect(component.openMilestones.length).toBe(0);
      expect(toastrService.error).toHaveBeenCalledWith('Could not load roadmap data.', 'Error');
    }));
  });
});
