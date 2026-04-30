import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { of, throwError, Subject } from 'rxjs';
import { delay } from 'rxjs/operators';
import { provideHttpClient } from '@angular/common/http';
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
  let mockGraphStateService: jasmine.SpyObj<GraphStateService>;
  let parameterMapSubject: Subject<any>;

  beforeEach(async () => {
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById', 'getAllReleases']);
    mockLabelService = jasmine.createSpyObj('LabelService', ['getHighLightsByReleaseId']);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockVulnerabilityService = jasmine.createSpyObj('VulnerabilityService', ['getVulnerabilitiesByReleaseId']);
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['getBusinessValuesByReleaseId']);
    mockLocation = jasmine.createSpyObj('Location', ['back']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockGraphStateService = jasmine.createSpyObj('GraphStateService', ['getGraphQueryParams']);
    mockGraphStateService.getGraphQueryParams.and.returnValue({});
    mockReleaseService.getAllReleases.and.returnValue(of([mockRelease]).pipe(delay(0)));

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
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseDetailsComponent);
    component = fixture.componentInstance;
  });

  function setupAndLoad(businessValues: BusinessValue[]): Promise<void> {
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
    mockReleaseService.getAllReleases.and.returnValue(of([mockRelease]).pipe(delay(0)));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(businessValues).pipe(delay(0)));
    return Promise.resolve();
  }

  function setupNightlyLoad(allReleases: Release[], currentRelease: Release = mockRelease): void {
    mockReleaseService.getReleaseById.and.returnValue(of(currentRelease).pipe(delay(0)));
    mockReleaseService.getAllReleases.and.returnValue(of(allReleases).pipe(delay(0)));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
  }

  function setupNavLoad(allReleases: Release[]): void {
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
    mockReleaseService.getAllReleases.and.returnValue(of(allReleases).pipe(delay(0)));
    mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit - Data Fetching', () => {
    it('should set isLoading to true initially, and then to false after data is fetched', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });

      expect(component.isLoading).toBe(true);

      tick();

      expect(component.isLoading).toBe(false);
    }));

    it('should fetch release data when a valid release ID is provided', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-1');
      expect(mockLabelService.getHighLightsByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockVulnerabilityService.getVulnerabilitiesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockBusinessValueService.getBusinessValuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(component.release).toEqual(mockRelease);
      expect(component.highlightedLabels).toEqual(mockLabels);
      expect(component.releaseIssues).toEqual(mockIssues);
      expect(component.vulnerabilities).toEqual(mockVulnerabilities);
      expect(component.businessValues).toEqual(mockBusinessValues);
    }));

    it('should handle release fetch error gracefully', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(
        throwError(() => new Error('Release API Error')).pipe(delay(0)),
      );

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.release).toBeUndefined();
    }));

    it('should handle label fetch error gracefully', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(
        throwError(() => new Error('Label API Error')).pipe(delay(0)),
      );
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.highlightedLabels).toBeNull();
      expect(component.releaseIssues).toEqual(mockIssues);
      expect(component.vulnerabilities).toEqual(mockVulnerabilities);
    }));

    it('should handle issue fetch error gracefully', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(
        throwError(() => new Error('Issue API Error')).pipe(delay(0)),
      );
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.releaseIssues).toBeNull();
      expect(component.highlightedLabels).toEqual(mockLabels);
      expect(component.vulnerabilities).toEqual(mockVulnerabilities);
    }));

    it('should set data to null if API returns an empty array for labels', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.highlightedLabels).toBeNull();
    }));

    it('should set data to undefined if API returns an empty array for issues', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.releaseIssues).toBeNull();
    }));

    it('should handle vulnerability fetch error gracefully', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(
        throwError(() => new Error('Vulnerability API Error')).pipe(delay(0)),
      );
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.vulnerabilities).toEqual([]);
    }));

    it('should set vulnerabilities to empty array if API returns empty array', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.vulnerabilities).toEqual([]);
    }));

    it('should refetch data when route parameter changes', fakeAsync(() => {
      const mockRelease2: Release = {
        id: 'release-2',
        name: 'v2.0.0',
        tagName: 'v2',
        publishedAt: new Date(),
        lastScanned: new Date(),
        branch: { id: 'b2', name: 'master' },
      };

      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.release).toEqual(mockRelease);

      // Change route parameter
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease2).pipe(delay(0)));
      parameterMapSubject.next({ get: () => 'release-2' });
      tick();

      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-2');
      expect(component.release).toEqual(mockRelease2);
    }));
  });

  describe('activeView behavior', () => {
    it('should set activeView to business-value when businessValues are loaded', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.businessValues).toEqual(mockBusinessValues);
      expect(component.activeView()).toBe('business-value');
    }));

    it('should set activeView to issues when businessValues is empty', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.businessValues).toBeNull();
      expect(component.activeView()).toBe('issues');
    }));

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

  describe('goBack', () => {
    it('should navigate to /graph when goBack is called', () => {
      component.goBack();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph'], { queryParams: {} });
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
    it('should show view toggle when businessValues are present', fakeAsync(() => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const toggle = fixture.nativeElement.querySelector('.section-view-toggle');

      expect(toggle).toBeTruthy();
    }));

    it('should not show view toggle when there are no businessValues', fakeAsync(() => {
      setupAndLoad([]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const toggle = fixture.nativeElement.querySelector('.section-view-toggle');

      expect(toggle).toBeNull();
    }));

    it('should render two tab buttons with correct labels when businessValues are present', fakeAsync(() => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs.length).toBe(2);
      expect(tabs[0].textContent.trim()).toBe('Business Values');
      expect(tabs[1].textContent.trim()).toBe('Important Issues');
    }));

    it('should have correct tooltip titles on toggle buttons', fakeAsync(() => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs[0].title).toBe('Grouped issues that contribute to one functional item.');
      expect(tabs[1].title).toBe('Subsets of issues that might be relevant to you.');
    }));

    it('should mark Business Values tab as active by default when businessValues are present', fakeAsync(() => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');

      expect(tabs[0].classList).toContain('active');
      expect(tabs[1].classList).not.toContain('active');
    }));

    it('should switch active tab when Important Issues is clicked', fakeAsync(() => {
      setupAndLoad(mockBusinessValues);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const tabs = fixture.nativeElement.querySelectorAll('.view-tab');
      tabs[1].click();
      fixture.detectChanges();

      expect(component.activeView()).toBe('issues');
      expect(tabs[1].classList).toContain('active');
      expect(tabs[0].classList).not.toContain('active');
    }));

    it('should show issues section when there are no businessValues', fakeAsync(() => {
      setupAndLoad([]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      expect(component.activeView()).toBe('issues');
      const issuesSection = fixture.nativeElement.querySelector('app-release-important-issues');

      expect(issuesSection).toBeTruthy();
    }));
  });

  describe('branch navigation', () => {
    it('should set previousRelease and nextRelease when current is in the middle of branch', fakeAsync(() => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.previousRelease()).toEqual(mockReleasePrevious);
      expect(component.nextRelease()).toEqual(mockReleaseNext);
    }));

    it('should set previousRelease to null when current is the first in branch', fakeAsync(() => {
      setupNavLoad([mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toEqual(mockReleaseNext);
    }));

    it('should set nextRelease to null when current is the last in branch', fakeAsync(() => {
      setupNavLoad([mockReleasePrevious, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.previousRelease()).toEqual(mockReleasePrevious);
      expect(component.nextRelease()).toBeNull();
    }));

    it('should set branchReleases sorted by publishedAt ascending', fakeAsync(() => {
      setupNavLoad([mockReleaseNext, mockRelease, mockReleasePrevious]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNext.id]);
    }));

    it('should not include releases from a different branch', fakeAsync(() => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext, mockReleaseOtherBranch]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseOtherBranch.id);
      expect(ids.length).toBe(3);
    }));

    it('should set branchReleases to single item and both nav signals null when only one release in branch', fakeAsync(() => {
      setupNavLoad([mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.branchReleases().length).toBe(1);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    }));

    it('should not render version-nav buttons when only one release in branch', fakeAsync(() => {
      setupNavLoad([mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(0);
    }));

    it('should render two version-nav buttons when current is in the middle', fakeAsync(() => {
      setupNavLoad([mockReleasePrevious, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(2);
      expect(buttons[0].textContent.trim()).toBe(mockReleasePrevious.name);
      expect(buttons[1].textContent.trim()).toBe(mockReleaseNext.name);
    }));

    it('should render only next button when current is the first in branch', fakeAsync(() => {
      setupNavLoad([mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(1);
      expect(buttons[0].textContent.trim()).toBe(mockReleaseNext.name);
    }));

    it('should render only prev button when current is the last in branch', fakeAsync(() => {
      setupNavLoad([mockReleasePrevious, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('.version-nav-btn');

      expect(buttons.length).toBe(1);
      expect(buttons[0].textContent.trim()).toBe(mockReleasePrevious.name);
    }));

    it('should gracefully handle getAllReleases error and leave navigation empty', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockReleaseService.getAllReleases.and.returnValue(throwError(() => new Error('Network error')).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.release).toEqual(mockRelease);
      expect(component.branchReleases()).toEqual([]);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    }));
  });

  describe('navigateToRelease', () => {
    it('should navigate stripping release/ prefix from tagName', () => {
      const releaseWithPrefix: Release = { ...mockReleaseNext, tagName: 'release/v1.1' };
      component.navigateToRelease(releaseWithPrefix);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', 'v1.1'], { queryParams: {} });
    });

    it('should navigate with tagName as-is when no release/ prefix', () => {
      component.navigateToRelease(mockReleaseNext);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', mockReleaseNext.tagName], { queryParams: {} });
    });

    it('should pass current graph query params when navigating', () => {
      mockGraphStateService.getGraphQueryParams.and.returnValue({ nightly: '' });
      component.navigateToRelease(mockReleaseNext);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', mockReleaseNext.tagName], {
        queryParams: { nightly: '' },
      });
    });

    it('should not navigate when release is null', () => {
      component.navigateToRelease(null);

      expect(mockRouter.navigate).not.toHaveBeenCalled();
    });
  });

  describe('nightly filtering in branch navigation', () => {
    it('should exclude a nightly that has a stable release after it', fakeAsync(() => {
      setupNightlyLoad([mockReleasePrevious, mockReleaseNightlyMid, mockRelease, mockReleaseNext]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNext.id]);
    }));

    it('should keep a nightly at the trailing end of the branch', fakeAsync(() => {
      setupNightlyLoad([mockReleasePrevious, mockRelease, mockReleaseNightlyEnd]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toEqual([mockReleasePrevious.id, mockRelease.id, mockReleaseNightlyEnd.id]);
    }));

    it('should set nextRelease to trailing nightly', fakeAsync(() => {
      setupNightlyLoad([mockReleasePrevious, mockRelease, mockReleaseNightlyEnd]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.nextRelease()).toEqual(mockReleaseNightlyEnd);
    }));

    it('should remove multiple mid-list nightlies but keep trailing ones', fakeAsync(() => {
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
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).not.toContain(anotherMidNightly.id);
      expect(ids).toContain(mockReleaseNightlyEnd.id);
      expect(ids.length).toBe(4);
    }));

    it('should correctly detect nightly from tagName with release/ prefix', fakeAsync(() => {
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
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(prefixedNightly.id);
    }));

    it('should keep current release when it is a trailing nightly', fakeAsync(() => {
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
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).toContain(nightlyCurrent.id);
      expect(component.previousRelease()).toEqual(mockReleaseNext);
      expect(component.nextRelease()).toBeNull();
    }));

    it('should have no nav when all releases in branch are mid-nightlies except current', fakeAsync(() => {
      setupNightlyLoad([mockReleaseNightlyMid, mockRelease]);
      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      const ids = component.branchReleases().map((r) => r.id);

      expect(ids).not.toContain(mockReleaseNightlyMid.id);
      expect(ids).toEqual([mockRelease.id]);
      expect(component.previousRelease()).toBeNull();
      expect(component.nextRelease()).toBeNull();
    }));
  });
});
