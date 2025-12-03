import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReleaseManageComponent } from './release-manage.component';
import { AuthService, User } from '../../services/auth.service';
import { ReleaseService, Release } from '../../services/release.service';
import { IssueService } from '../../services/issue.service';
import { VulnerabilityService } from '../../services/vulnerability.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Input } from '@angular/core';

import { ReleaseBusinessValueComponent } from '../release-details/release-business-value/release-business-value.component';
import { ReleaseImportantIssuesComponent } from '../release-details/release-important-issues/release-important-issues.component';
import { ReleaseVulnerabilities } from '../release-details/release-vulnerabilities/release-vulnerabilities';

@Component({ selector: 'app-release-business-value', standalone: true, template: '' })
class MockReleaseBusinessValueComponent {
  @Input() releaseId: any;
}

@Component({ selector: 'app-release-important-issues', standalone: true, template: '' })
class MockReleaseImportantIssuesComponent {
  @Input() releaseIssues: any;
  @Input() releaseId: any;
}

@Component({ selector: 'app-release-vulnerabilities', standalone: true, template: '' })
class MockReleaseVulnerabilities {
  @Input() vulnerabilities: any;
}

describe('ReleaseManageComponent', () => {
  let component: ReleaseManageComponent;
  let fixture: ComponentFixture<ReleaseManageComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockIssueService: jasmine.SpyObj<IssueService>;
  let mockVulnerabilityService: jasmine.SpyObj<VulnerabilityService>;
  let mockRouter: jasmine.SpyObj<Router>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://example.com/avatar.jpg',
    isFrankFrameworkMember: true,
  };

  const mockRelease: Release = {
    id: '123',
    tagName: 'v1.0.0',
    name: 'Release 1.0.0',
    publishedAt: new Date('2024-01-01'),
    branch: { id: '1', name: 'main' },
  };

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal<User | null>(mockUser),
    });
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById']);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockVulnerabilityService = jasmine.createSpyObj('VulnerabilityService', ['getVulnerabilitiesByReleaseId']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of([]));
    mockVulnerabilityService.getVulnerabilitiesByReleaseId.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ReleaseManageComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: VulnerabilityService, useValue: mockVulnerabilityService },
        { provide: Router, useValue: mockRouter },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => '123',
              },
            },
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })
      .overrideComponent(ReleaseManageComponent, {
        remove: {
          imports: [
            ReleaseBusinessValueComponent,
            ReleaseImportantIssuesComponent,
            ReleaseVulnerabilities
          ]
        },
        add: {
          imports: [
            MockReleaseBusinessValueComponent,
            MockReleaseImportantIssuesComponent,
            MockReleaseVulnerabilities
          ]
        }
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReleaseManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch release data, issues, and vulnerabilities on init', () => {
    expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('123');
    expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('123');
    expect(mockVulnerabilityService.getVulnerabilitiesByReleaseId).toHaveBeenCalledWith('123');
    expect(component.release()).toEqual(mockRelease);
    expect(component.isLoading()).toBeFalse();
  });

  it('should display the release name in the header', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('.header-details h1');

    expect(heading?.textContent).toContain('Release 1.0.0');
  });

  it('should display management section cards in overview', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const businessCard = compiled.querySelector('.business-value-card');
    const vulnCard = compiled.querySelector('.vulnerabilities-card');

    expect(businessCard).toBeTruthy();
    expect(vulnCard).toBeTruthy();
    expect(businessCard?.textContent).toContain('Business Values');
    expect(vulnCard?.textContent).toContain('Vulnerabilities');
  });

  it('should navigate to business values route when business value card is clicked', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const card = compiled.querySelector('.business-value-card') as HTMLElement;

    card.click();
    fixture.detectChanges();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/release-manage', '123', 'business-values']);
  });

  it('should navigate to vulnerabilities when vulnerabilities card is clicked', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const card = compiled.querySelector('.vulnerabilities-card') as HTMLElement;

    card.click();
    fixture.detectChanges();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/release-manage', '123', 'vulnerabilities']);
  });

  it('should close section and return to overview when close button is clicked', () => {
    component.activeSection.set('vulnerabilities');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const closeButton = compiled.querySelector('.close-button') as HTMLElement;

    expect(closeButton).toBeTruthy();
    closeButton.click();
    fixture.detectChanges();

    expect(component.activeSection()).toBeNull();
    expect(compiled.querySelector('.management-sections')).toBeTruthy();
  });

  it('should navigate back to graph with release ID when goBack is called', () => {
    component.goBack();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', '123']);
  });

  it('should navigate back to general graph if release is null', () => {
    component.release.set(null);
    component.goBack();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph']);
  });

  it('should navigate to not-found on fetch error', () => {
    mockReleaseService.getReleaseById.and.returnValue(throwError(() => new Error('Not found')));

    component.ngOnInit();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/not-found']);
    expect(component.isLoading()).toBeFalse();
  });
});
