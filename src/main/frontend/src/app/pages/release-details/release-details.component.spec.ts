import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
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
import { GitHubStates } from '../../app.service';

const mockRelease: Release = {
  id: 'release-1',
  name: 'v1.0.0',
  tagName: 'v1',
  publishedAt: new Date(),
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
    cwes: ['CWE-79']
  }
];

describe('ReleaseDetailsComponent', () => {
  let component: ReleaseDetailsComponent;
  let fixture: ComponentFixture<ReleaseDetailsComponent>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockLabelService: jasmine.SpyObj<LabelService>;
  let mockIssueService: jasmine.SpyObj<IssueService>;
  let mockVulnerabilityService: jasmine.SpyObj<VulnerabilityService>;
  let mockLocation: jasmine.SpyObj<Location>;
  let parameterMapSubject: Subject<any>;

  beforeEach(async () => {
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById']);
    mockLabelService = jasmine.createSpyObj('LabelService', ['getHighLightsByReleaseId']);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockVulnerabilityService = jasmine.createSpyObj('VulnerabilityService', ['getVulnerabilitiesByReleaseId']);
    mockLocation = jasmine.createSpyObj('Location', ['back']);

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
        { provide: Location, useValue: mockLocation },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseDetailsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit - Data Fetching', () => {
    it('should set isLoading to true initially, and then to false after data is fetched', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));

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

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-1');
      expect(mockLabelService.getHighLightsByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockVulnerabilityService.getVulnerabilitiesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(component.release).toEqual(mockRelease);
      expect(component.highlightedLabels).toEqual(mockLabels);
      expect(component.releaseIssues).toEqual(mockIssues);
      expect(component.vulnerabilities).toEqual(mockVulnerabilities);
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

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);

      expect(component.highlightedLabels).toBeUndefined();
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

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.releaseIssues).toBeUndefined();
      expect(component.highlightedLabels).toEqual(mockLabels);
      expect(component.vulnerabilities).toEqual(mockVulnerabilities);
    }));

    it('should set data to undefined if API returns an empty array for labels', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.highlightedLabels).toBeUndefined();
    }));

    it('should set data to undefined if API returns an empty array for issues', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));

      fixture.detectChanges();
      parameterMapSubject.next({ get: () => 'release-1' });
      tick();

      expect(component.releaseIssues).toBeUndefined();
    }));

    it('should handle vulnerability fetch error gracefully', fakeAsync(() => {
      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(
        throwError(() => new Error('Vulnerability API Error')).pipe(delay(0)),
      );

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
        branch: { id: 'b2', name: 'master' },
      };

      mockReleaseService.getReleaseById.and.returnValue(of(mockRelease).pipe(delay(0)));
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));
      mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of(mockVulnerabilities).pipe(delay(0)));

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

  describe('goBack', () => {
    it('should call location.back() when goBack is called', () => {
      component.goBack();

      expect(mockLocation.back).toHaveBeenCalledWith();
    });
  });
});
