import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReleaseRoadmapComponent } from './release-roadmap.component';
import { MilestoneService, Milestone } from '../../services/milestone.service'; // Import Milestone
import { IssueService, Issue } from '../../services/issue.service';
import { GitHubStates } from '../../app.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const BASE_MOCK_UNPLANNED_EPICS: Issue[] = [
  {
    id: 'epic3',
    number: 300,
    title: 'Epic Three',
    state: GitHubStates.OPEN,
    url: 'http://example.com/epic3',
  } as Issue,
  {
    id: 'epic1',
    number: 100,
    title: 'Epic One',
    state: GitHubStates.OPEN,
    url: 'http://example.com/epic1',
  } as Issue,
];

const getEpicsCopy = () => BASE_MOCK_UNPLANNED_EPICS.map(epic => ({ ...epic }));

describe('ReleaseRoadmapComponent', () => {
  let component: ReleaseRoadmapComponent;
  let fixture: ComponentFixture<ReleaseRoadmapComponent>;
  let milestoneService: jasmine.SpyObj<MilestoneService>;
  let issueService: jasmine.SpyObj<IssueService>;

  beforeEach(async () => {
    const milestoneSpy = jasmine.createSpyObj('MilestoneService', ['getMilestones']);
    const issueSpy = jasmine.createSpyObj('IssueService', ['getIssuesByMilestoneId', 'getFutureEpicIssues']);

    await TestBed.configureTestingModule({
      imports: [ReleaseRoadmapComponent, NoopAnimationsModule],
      providers: [
        { provide: MilestoneService, useValue: milestoneSpy },
        { provide: IssueService, useValue: issueSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseRoadmapComponent);
    component = fixture.componentInstance;
    milestoneService = TestBed.inject(MilestoneService) as jasmine.SpyObj<MilestoneService>;
    issueService = TestBed.inject(IssueService) as jasmine.SpyObj<IssueService>;

    milestoneService.getMilestones.and.returnValue(of([]));
    issueService.getIssuesByMilestoneId.and.returnValue(of([]));
    issueService.getFutureEpicIssues.and.callFake(() => of(getEpicsCopy()));
  });

  it('should create', () => {
    jasmine.clock().mockDate(new Date(2025, 6, 10, 12, 0, 0));
    issueService.getFutureEpicIssues.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Unplanned Epics Milestone', () => {
    beforeEach(fakeAsync(() => {
      jasmine.clock().mockDate(new Date(2025, 6, 10, 12, 0, 0));

      const MOCK_MILESTONE = {
        id: 'm1',
        title: 'dummy milestone',
        dueOn: new Date(),
        openIssueCount: 0,
        closedIssueCount: 0,
        state: GitHubStates.OPEN
      } as Milestone;

      milestoneService.getMilestones.and.returnValue(of([MOCK_MILESTONE]));
      issueService.getIssuesByMilestoneId.withArgs('m1').and.returnValue(of([]));

      fixture.detectChanges();
      tick();
      fixture.detectChanges();
    }));

    it('should create a special milestone for unplanned epics', () => {
      const unplannedMilestone = component.milestones.find((m) => m.id === 'unplanned-epics');

      expect(unplannedMilestone).toBeDefined();
      expect(unplannedMilestone?.title).toBe('Unplanned Epics');
      expect(unplannedMilestone?.openIssueCount).toBe(2);
    });

    it('should sort unplanned epics by issue number ascending', () => {
      const unplannedEpicsIssues = component.milestoneIssues.get('unplanned-epics');

      expect(unplannedEpicsIssues).toBeDefined();
      expect(unplannedEpicsIssues?.length).toBe(2);
      expect(unplannedEpicsIssues?.[0].number).toBe(100);
      expect(unplannedEpicsIssues?.[1].number).toBe(300);
    });
  });
});
