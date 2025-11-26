import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReleaseManageComponent } from './release-manage.component';
import { AuthService, User } from '../../services/auth.service';
import { ReleaseService, Release } from '../../services/release.service';
import { IssueService } from '../../services/issue.service';
import { VulnerabilityService } from '../../services/vulnerability.service';

describe('ReleaseManageComponent', () => {
  let component: ReleaseManageComponent;
  let fixture: ComponentFixture<ReleaseManageComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockIssueService: jasmine.SpyObj<IssueService>;
  let mockVulnerabilityService: jasmine.SpyObj<VulnerabilityService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: { snapshot: { paramMap: { get: jasmine.Spy } } };

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
    publishedAt: '2024-01-01',
    branch: { name: 'main' },
  };

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal<User | null>(mockUser),
    });
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById']);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockVulnerabilityService = jasmine.createSpyObj('VulnerabilityService', ['getVulnerabilitiesByReleaseId']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('123'),
        },
      },
    };

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
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch release data on init', () => {
    expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('123');
    expect(component.release()).toEqual(mockRelease);
  });

  it('should display the username in the welcome message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const welcomeMessage = compiled.querySelector('.welcome-message strong');

    expect(welcomeMessage?.textContent).toContain('testuser');
  });

  it('should display the release name in the heading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');

    expect(heading?.textContent).toContain('Release 1.0.0');
  });

  it('should display management cards', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('.management-card h2');

    expect(cards[0]?.textContent).toBe('Business Values & Important Issues');
    expect(cards[1]?.textContent).toBe('CVE & Vulnerabilities');
  });

  it('should open business value section when card is clicked', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const card = compiled.querySelectorAll('.management-card')[0] as HTMLElement;

    card.click();
    fixture.detectChanges();

    expect(component.activeSection()).toBe('business-value');
  });

  it('should open vulnerabilities section when card is clicked', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const card = compiled.querySelectorAll('.management-card')[1] as HTMLElement;

    card.click();
    fixture.detectChanges();

    expect(component.activeSection()).toBe('vulnerabilities');
  });

  it('should close section and return to overview', () => {
    component.openSection('business-value');
    fixture.detectChanges();

    component.closeSection();
    fixture.detectChanges();

    expect(component.activeSection()).toBeNull();
  });

  it('should navigate back to release details', () => {
    component.goBack();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', '123']);
  });

  it('should navigate to not-found on error', () => {
    mockReleaseService.getReleaseById.and.returnValue(throwError(() => new Error('Not found')));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/not-found']);
  });
});
