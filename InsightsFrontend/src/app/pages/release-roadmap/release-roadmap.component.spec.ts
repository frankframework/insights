import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ReleaseRoadmapComponent } from './release-roadmap.component';
import { MilestoneService, Milestone } from '../../services/milestone.service';
import { IssueService, Issue } from '../../services/issue.service';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';

const mockMilestoneService = {
  getOpenMilestones: (): Observable<Milestone[]> => of([]),
};

const mockIssueService = {
  getIssuesByMilestoneId: (): Observable<Issue[]> => of([]),
};

const mockToastrService = {
  error: jasmine.createSpy('error'),
};

const MOCK_MILESTONES: Milestone[] = [
  {
    id: 'm1',
    title: 'Release 9.2.0',
    dueOn: null,
    openIssueCount: 1,
    closedIssueCount: 1,
    url: '',
    number: 0,
    state: 'OPEN',
    major: 0,
    minor: 0,
    patch: 0,
  },
  {
    id: 'm2',
    title: 'Release 9.1.0',
    dueOn: null,
    openIssueCount: 2,
    closedIssueCount: 0,
    url: '',
    number: 0,
    state: 'OPEN',
    major: 0,
    minor: 0,
    patch: 0,
  },
];

const MOCK_ISSUES: Issue[] = [{ id: 'i1', number: 1, title: 'Test issue', state: 'OPEN', url: '', points: 5 } as Issue];

describe('ReleaseRoadmapComponent', () => {
  let component: ReleaseRoadmapComponent;
  let fixture: ComponentFixture<ReleaseRoadmapComponent>;
  let milestoneService: MilestoneService;
  let toastrService: ToastrService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseRoadmapComponent],
      providers: [
        { provide: MilestoneService, useValue: mockMilestoneService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: ToastrService, useValue: mockToastrService },
      ],
    })
      .overrideComponent(ReleaseRoadmapComponent, {
        set: {
          imports: [RoadmapToolbarComponent, TimelineHeaderComponent, MilestoneRowComponent, LoaderComponent],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReleaseRoadmapComponent);
    component = fixture.componentInstance;
    milestoneService = TestBed.inject(MilestoneService);
    toastrService = TestBed.inject(ToastrService);
  });

  afterEach(() => {
    (toastrService.error as jasmine.Spy).calls.reset();
  });

  it('should create', () => {
    spyOn(milestoneService, 'getOpenMilestones').and.returnValue(of([]));
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Timeline Period Calculation', () => {
    beforeEach(() => {
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date('2025-06-25T12:00:00.000Z'));
      spyOn(milestoneService, 'getOpenMilestones').and.returnValue(of([]));
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('should set displayDate to the start of the current quarter on resetPeriod', () => {
      component.resetPeriod();

      expect(component.displayDate.getFullYear()).toBe(2025);
      expect(component.displayDate.getMonth()).toBe(3);
      expect(component.displayDate.getDate()).toBe(1);
    });

    it('should advance displayDate by 3 months when changePeriod(3) is called', () => {
      component.resetPeriod(); // Start at April 1, 2025
      component.changePeriod(3);
      // Should now be July 1, 2025
      expect(component.displayDate.getFullYear()).toBe(2025);
      expect(component.displayDate.getMonth()).toBe(6); // 6 is July
      expect(component.displayDate.getDate()).toBe(1);
    });

    it('should generate 6 months and 2 quarters based on the displayDate', () => {
      component.resetPeriod();

      expect(component.months.length).toBe(6);
      expect(component.quarters.length).toBe(2);
      expect(component.quarters[0].name).toBe('Q2 2025');
      expect(component.quarters[1].name).toBe('Q3 2025');
    });
  });

  describe('Data Loading', () => {
    it('should load milestones and issues successfully', () => {
      spyOn(milestoneService, 'getOpenMilestones').and.returnValue(of(MOCK_MILESTONES));
      spyOn(mockIssueService, 'getIssuesByMilestoneId').and.returnValue(of(MOCK_ISSUES));

      fixture.detectChanges();

      expect(component.isLoading).toBeFalse();
      expect(component.openMilestones.length).toBeGreaterThan(0);
      expect(component.milestoneIssues.get('m1')).toEqual(MOCK_ISSUES);
      expect(toastrService.error).not.toHaveBeenCalled();
    });

    it('should handle errors during data loading and show a toastr message', () => {
      spyOn(milestoneService, 'getOpenMilestones').and.returnValue(throwError(() => new Error('API Error')));

      fixture.detectChanges();

      expect(component.isLoading).toBeFalse();
      expect(component.openMilestones.length).toBe(0);
      expect(toastrService.error).toHaveBeenCalledWith('Could not load roadmap data.', 'Error');
    });
  });

  describe('Milestone Scheduling Logic', () => {
    it('should schedule milestones by setting their dueOn date', () => {
      const unscheduledMilestones = [
        { id: 'm1', title: 'Release 9.2.0', dueOn: null } as Milestone,
        { id: 'm2', title: 'Release 9.1.0', dueOn: null } as Milestone,
        { id: 'm3', title: 'Release 9.1.1', dueOn: null } as Milestone,
      ];
      spyOn(milestoneService, 'getOpenMilestones').and.returnValue(of(unscheduledMilestones));
      spyOn(mockIssueService, 'getIssuesByMilestoneId').and.returnValue(of([]));

      fixture.detectChanges();

      expect(component.openMilestones.length).toBeGreaterThan(0);
      expect(component.openMilestones.every((m) => m.dueOn !== null)).toBeTrue();
    });
  });
});
