import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, DefaultUrlSerializer, Router, UrlTree } from '@angular/router';
import { Location } from '@angular/common';
import { of, throwError, Subject } from 'rxjs';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReleaseDetailsComponent } from './release-details.component';
import { ReleaseService, Release } from '../../services/release.service';
import { LabelService, Label } from '../../services/label.service';
import { IssueService, Issue } from '../../services/issue.service';
import { VulnerabilityService, Vulnerability, VulnerabilitySeverities } from '../../services/vulnerability.service';
import { BusinessValueService, BusinessValue } from '../../services/business-value.service';
import { GraphStateService } from '../../services/graph-state.service';
import { GitHubStates } from '../../app.service';

const mockRelease: Release = {
  id: 'release-1',
  name: 'v1.0.0',
  tagName: 'v1',
  publishedAt: new Date('2024-02-01'),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'master' },
};

const mockLabels: Label[] = [{ id: 'label-1', name: 'Highlight', color: '0000ff', description: '' }];
const mockIssues: Issue[] = [{ id: 'issue-1', number: 123, title: 'Test Issue', state: GitHubStates.OPEN, url: '' }];
const mockVulnerabilities: Vulnerability[] = [
  {
    cveId: 'CVE-2024-0001',
    title: 'title',
    severity: VulnerabilitySeverities.CRITICAL,
    cvssScore: 9.8,
    description: 'Critical vulnerability',
    cwes: ['CWE-79'],
  },
];
const mockBusinessValues: BusinessValue[] = [
  { id: 'bv-1', title: 'Business Value 1', description: 'Description 1', releaseId: 'bv-release-1', issues: [] },
];

const mockReleasePrevious: Release = {
  id: 'release-0',
  name: 'v0.9.0',
  tagName: 'v0.9',
  publishedAt: new Date('2024-01-01'),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'master' },
};

const mockReleaseNext: Release = {
  id: 'release-2',
  name: 'v1.1.0',
  tagName: 'v1.1',
  publishedAt: new Date('2024-03-01'),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'master' },
};

const mockReleaseOtherBranch: Release = {
  id: 'release-other',
  name: 'v2.0.0',
  tagName: 'v2',
  publishedAt: new Date('2024-02-15'),
  lastScanned: new Date(),
  branch: { id: 'b2', name: '2.x' },
};

const mockReleaseNightlyMid: Release = {
  id: 'release-nightly-mid',
  name: 'v1.0.0-nightly',
  tagName: 'release/v1.0.0-nightly',
  publishedAt: new Date('2024-01-15'),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'master' },
};

const mockReleaseNightlyEnd: Release = {
  id: 'release-nightly-end',
  name: 'v1.1.0-nightly',
  tagName: 'release/v1.1.0-nightly',
  publishedAt: new Date('2024-04-01'),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'master' },
};

const urlSerializer = new DefaultUrlSerializer();

describe('ReleaseDetailsComponent', () => {
  let component: ReleaseDetailsComponent;
  let fixture: ComponentFixture<ReleaseDetailsComponent>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockLabelService: jasmine.SpyObj<LabelService>;
  let mockIssueService: jasmine.SpyObj<IssueService>;
  let mockVulnerabilityService: jasmine.SpyObj<VulnerabilityService>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;
  let mockLocation: jasmine.SpyObj<Location>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockGraphStateService: Pick<GraphStateService, 'graphQueryParams'>;
  let graphQueryParametersState: WritableSignal<Record<string, string>>;
  let parameterMapSubject: Subject<any>;

  beforeEach(async () => {
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById', 'getAllReleases']);
    mockLabelService = jasmine.createSpyObj('LabelService', ['getHighLightsByReleaseId']);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockVulnerabilityService = jasmine.createSpyObj('VulnerabilityService', ['getVulnerabilitiesByReleaseId']);
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['getBusinessValuesByReleaseId']);
    mockLocation = jasmine.createSpyObj('Location', ['back']);
    mockRouter = jasmine.createSpyObj('Router', [
      'navigate',
      'navigateByUrl',
      'parseUrl',
      'createUrlTree',
      'serializeUrl',
    ]);
    mockRouter.parseUrl.and.callFake((url: string) => urlSerializer.parse(url));
    mockRouter.createUrlTree.and.callFake((commands: string[]) => urlSerializer.parse(commands.join('') || '/'));
    mockRouter.serializeUrl.and.callFake((tree: UrlTree) => urlSerializer.serialize(tree));
    graphQueryParametersState = signal<Record<string, string>>({});
    mockGraphStateService = { graphQueryParams: graphQueryParametersState.asReadonly() };
    mockReleaseService.getAllReleases.and.returnValue(of([mockRelease]));

    parameterMapSubject = new Subject();

    const mockActivatedRoute = {
      paramMap: parameterMapSubject.asObservable(),
    };

    await TestBed.configureTestingModule({
      imports: [ReleaseDetailsComponent],
      providers: [
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: LabelService, useValue: mockLabelService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: VulnerabilityService, useValue: mockVulnerabilityService },
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        { provide: Location, useValue: mockLocation },
        { provide: Router, useValue: mockRouter },
        { provide: GraphStateService, useValue: mockGraphStateService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseDetailsComponent);
    component = fixture.componentInstance;
  });

  function setupAndLoad(businessValues: BusinessValue[]): void {
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
    mockReleaseService.getAllReleases.and.returnValue(of([mockRelease]));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(businessValues));
  }

  function setupNightlyLoad(allReleases: Release[], currentRelease: Release = mockRelease): void {
    mockReleaseService.getReleaseById.and.returnValue(of(currentRelease));
    mockReleaseService.getAllReleases.and.returnValue(of(allReleases));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of([]));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]));
  }

  function setupNavLoad(allReleases: Release[]): void {
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
    mockReleaseService.getAllReleases.and.returnValue(of(allReleases));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of([]));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]));
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit - Data Fetching', () => {
    it('should set isLoading to true initially, and then to false after data is fetched', () => {
      // Held open so the loading state is observable before the release resolves.
      const releaseSubject = new Subject<Release>();
      mockReleaseService.getReleaseById.and.returnValue(releaseSubject);
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading()).toBe(true);

      releaseSubject.next(mockRelease);
      releaseSubject.complete();

      expect(component.isLoading()).toBe(false);
    });

    it('should fetch release data when a valid release ID is provided', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-1');
      expect(mockLabelService.getHighLightsByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockVulnerabilityService.getVulnerabilitiesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockBusinessValueService.getBusinessValuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(component.release()).toEqual(mockRelease);
      expect(component.highlightedLabels()).toEqual(mockLabels);
      expect(component.releaseIssues()).toEqual(mockIssues);
      expect(component.vulnerabilities()).toEqual(mockVulnerabilities);
      expect(component.businessValues()).toEqual(mockBusinessValues);
    });

    it('should handle release fetch error gracefully', () => {
      mockReleaseService.getReleaseById.and.returnValue(
        throwError(() => new Error('Release API Error')),
      );

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading()).toBe(false);
      expect(component.release()).toBeNull();
    });

    it('should handle label fetch error gracefully', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(
        throwError(() => new Error('Label API Error')),
      );
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading()).toBe(false);
      expect(component.highlightedLabels()).toBeNull();
      expect(component.releaseIssues()).toEqual(mockIssues);
      expect(component.vulnerabilities()).toEqual(mockVulnerabilities);
    });

    it('should handle issue fetch error gracefully', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(
        throwError(() => new Error('Issue API Error')),
      );
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading()).toBe(false);
      expect(component.releaseIssues()).toBeNull();
      expect(component.highlightedLabels()).toEqual(mockLabels);
      expect(component.vulnerabilities()).toEqual(mockVulnerabilities);
    });

    it('should set data to null if API returns an empty array for labels', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.highlightedLabels()).toBeNull();
    });

    it('should set data to undefined if API returns an empty array for issues', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.releaseIssues()).toBeNull();
    });

    it('should handle vulnerability fetch error gracefully', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(
        throwError(() => new Error('Vulnerability API Error')),
      );
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading()).toBe(false);
      expect(component.vulnerabilities()).toEqual([]);
    });

    it('should set vulnerabilities to empty array if API returns empty array', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.vulnerabilities()).toEqual([]);
    });

    it('should refetch data when route parameter changes', () => {
      const mockRelease2: Release = {
        id: 'release-2',
        name: 'v2.0.0',
        tagName: 'v2',
        publishedAt: new Date(),
        lastScanned: new Date(),
        branch: { id: 'b2', name: 'master' },
      };

      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.release()).toEqual(mockRelease);

      // Change route parameter
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease2));
      parameterMapSubject.next({ get: () => 'release-2' });

      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-2');
      expect(component.release()).toEqual(mockRelease2);
    });
  });

  describe('activeView behavior', () => {
    it('should set activeView to business-value when businessValues are loaded', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.businessValues()).toEqual(mockBusinessValues);
      expect(component.activeView()).toBe('business-value');
    });

    it('should set activeView to issues when businessValues is empty', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.businessValues()).toBeNull();
      expect(component.activeView()).toBe('issues');
    });

    it('should switch view when setActiveView is called', () => {
      component.activeView.set('business-value');

      component.setActiveView('issues');

      expect(component.activeView()).toBe('issues');
    });

    it('should switch back to business-value view', () => {
      component.activeView.set('issues');

      component.setActiveView('business-value');

      expect(component.activeView()).toBe('business-value');
    });
  });

  describe('releaseGraphLink', () => {
    it('should return empty string when release is null', () => {
      expect(component.releaseGraphLink(null)).toBe('');
    });

    it('should strip release/ prefix from tagName', () => {
      const releaseWithPrefix: Release = { ...mockReleaseNext, tagName: 'release/v1.1' };

      expect(component.releaseGraphLink(releaseWithPrefix)).toBe('/graph/v1.1');
    });

    it('should use tagName as-is when no release/ prefix', () => {
      expect(component.releaseGraphLink(mockReleaseNext)).toBe(`/graph/${mockReleaseNext.tagName}`);
    });
  });

  describe('graphQueryParams', () => {
    it('should return current graph query params', () => {
      graphQueryParametersState.set({ nightly: '' });

      expect(component.graphQueryParams()).toEqual({ nightly: '' });
    });

    it('should return empty object when no params', () => {
      graphQueryParametersState.set({});

      expect(component.graphQueryParams()).toEqual({});
    });
  });

  describe('Signal initial states', () => {
    it('should initialize activeView to issues', () => {
      expect(component.activeView()).toBe('issues');
    });

    it('should initialize previousRelease to null', () => {
      expect(component.previousRelease()).toBeNull();
    });

    it('should initialize nextRelease to null', () => {
      expect(component.nextRelease()).toBeNull();
    });

    it('should initialize branchReleases to empty array', () => {
      expect(component.branchReleases()).toEqual([]);
    });
  });

  describe('view toggle template', () => {
    it('should show view toggle when businessValues are present', () => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const toggle = fixture.nativeElement.querySelector('.section-view-toggle');

      expect(toggle).toBeTruthy();
    });

    it('should not show view toggle when there are no businessValues', () => {
      setupAndLoad([]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const toggle = fixture.nativeElement.querySelector('.section-view-toggle');

      expect(toggle).toBeNull();
    });

    it('should render two tab buttons with correct labels when businessValues are present', () => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs.length).toBe(2);
      expect(tabs[0].textContent.trim()).toBe('Business Values');
      expect(tabs[1].textContent.trim()).toBe('Important Issues');
    });

    it('should have correct tooltip titles on toggle buttons', () => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs[0].title).toBe('Grouped issues that contribute to one functional item.');
      expect(tabs[1].title).toBe('Subsets of issues that might be relevant to you.');
    });

    it('should mark Business Values tab as active by default when businessValues are present', () => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs[0].classList).toContain('active');
      expect(tabs[1].classList).not.toContain('active');
    });

    it('should switch active tab when Important Issues is clicked', () => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');
      tabs[1].click();
      fixture.detectChanges();

      expect(component.activeView()).toBe('issues');
      expect(tabs[1].classList).toContain('active');
      expect(tabs[0].classList).not.toContain('active');
    });

    it('should show issues section when there are no businessValues', () => {
      setupAndLoad([]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      expect(component.activeView()).toBe('issues');
      const issuesSection = fixture.nativeElement.querySelector('app-release-important-issues');

      expect(issuesSection).toBeTruthy();
    });
  });

  describe('branch navigation', () => {
    it('should set previousRelease and nextRelease when current is in the middle of branch', () => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.previousRelease()).toEqual(mockReleasePrevious);
      expect(component.nextRelease()).toEqual(mockReleaseNext);
    });

    it('should set previousRelease to null when current is the first in branch', () => {
      setupNavLoad([mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toEqual(mockReleaseNext);
    });

    it('should set nextRelease to null when current is the last in branch', () => {
      setupNavLoad([mockReleasePrevious, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.previousRelease()).toEqual(mockReleasePrevious);
      expect(component.nextRelease()).toBeNull();
    });

    it('should set branchReleases sorted by publishedAt ascending', () => {
      setupNavLoad([mockReleaseNext, mockRelease, mockReleasePrevious]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNext.id]);
    });

    it('should not include releases from a different branch', () => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext, mockReleaseOtherBranch]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseOtherBranch.id);
      expect(ids.length).toBe(3);
    });

    it('should set branchReleases to single item and both nav signals null when only one release in branch', () => {
      setupNavLoad([mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.branchReleases().length).toBe(1);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    });

    it('should not render version-nav buttons when only one release in branch', () => {
      setupNavLoad([mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(0);
    });

    it('should render two version-nav buttons when current is in the middle', () => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(2);
      expect(buttons[0].textContent.trim()).toBe(mockReleasePrevious.name);
      expect(buttons[1].textContent.trim()).toBe(mockReleaseNext.name);
    });

    it('should render only next button when current is the first in branch', () => {
      setupNavLoad([mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(1);
      expect(buttons[0].textContent.trim()).toBe(mockReleaseNext.name);
    });

    it('should render only prev button when current is the last in branch', () => {
      setupNavLoad([mockReleasePrevious, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(1);
      expect(buttons[0].textContent.trim()).toBe(mockReleasePrevious.name);
    });

    it('should gracefully handle getAllReleases error and leave navigation empty', () => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
      mockReleaseService.getAllReleases.and.returnValue(throwError(() => new Error('Network error')));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.release()).toEqual(mockRelease);
      expect(component.branchReleases()).toEqual([]);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    });
  });

  describe('nightly filtering in branch navigation', () => {
    it('should exclude a nightly that has a stable release after it', () => {
      setupNightlyLoad([mockReleasePrevious, mockReleaseNightlyMid, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNext.id]);
    });

    it('should keep a nightly at the trailing end of the branch', () => {
      setupNightlyLoad([mockReleasePrevious, mockRelease, mockReleaseNightlyEnd]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNightlyEnd.id]);
    });

    it('should set nextRelease to trailing nightly', () => {
      setupNightlyLoad([mockReleasePrevious, mockRelease, mockReleaseNightlyEnd]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.nextRelease()).toEqual(mockReleaseNightlyEnd);
    });

    it('should remove multiple mid-list nightlies but keep trailing ones', () => {
      const anotherMidNightly: Release = {
        id: 'release-nightly-mid2',
        name: 'v0.9.5-nightly',
        tagName: 'release/v0.9.5-nightly',
        publishedAt: new Date('2024-01-20'),
        lastScanned: new Date(),
        branch: { id: 'b1', name: 'master' },
      };
      setupNightlyLoad([
        mockReleasePrevious,
        mockReleaseNightlyMid,
        anotherMidNightly,
        mockRelease,
        mockReleaseNext,
        mockReleaseNightlyEnd,
      ]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).not.toContain(anotherMidNightly.id);
      expect(ids).toContain(mockReleaseNightlyEnd.id);
      expect(ids.length).toBe(4);
    });

    it('should correctly detect nightly from tagName with release/ prefix', () => {
      const prefixedNightly: Release = {
        id: 'release-prefixed',
        name: '9.0-nightly',
        tagName: 'release/9.0-nightly',
        publishedAt: new Date('2024-01-10'),
        lastScanned: new Date(),
        branch: { id: 'b1', name: 'master' },
      };
      setupNightlyLoad([prefixedNightly, mockReleasePrevious, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(prefixedNightly.id);
    });

    it('should keep current release when it is a trailing nightly', () => {
      const nightlyCurrent: Release = {
        id: 'release-nightly-current',
        name: 'v1.2.0-nightly',
        tagName: 'release/v1.2.0-nightly',
        publishedAt: new Date('2024-04-01'),
        lastScanned: new Date(),
        branch: { id: 'b1', name: 'master' },
      };
      setupNightlyLoad([mockRelease, mockReleaseNext, nightlyCurrent], nightlyCurrent);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-nightly-current' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toContain(nightlyCurrent.id);
      expect(component.previousRelease()).toEqual(mockReleaseNext);
      expect(component.nextRelease()).toBeNull();
    });

    it('should have no nav when all releases in branch are mid-nightlies except current', () => {
      setupNightlyLoad([mockReleaseNightlyMid, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).toEqual([mockRelease.id]);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    });
  });
});
