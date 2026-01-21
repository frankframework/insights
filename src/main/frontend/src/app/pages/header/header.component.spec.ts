import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { AuthService, User } from '../../services/auth.service';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { signal, WritableSignal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { LocationService } from '../../services/location.service';
import { VersionService, BuildInfo } from '../../services/version.service';
import { GraphStateService } from '../../services/graph-state.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let mockLocationService: jasmine.SpyObj<LocationService>;
  let mockVersionService: jasmine.SpyObj<VersionService>;
  let mockGraphStateService: jasmine.SpyObj<GraphStateService>;
  let mockAuthService: {
    currentUser: WritableSignal<User | null>;
    isAuthenticated: WritableSignal<boolean>;
    authError: WritableSignal<string | null>;
    isLoading: WritableSignal<boolean>;
    setLoading: jasmine.Spy;
    logout: jasmine.Spy;
    clearError: jasmine.Spy;
  };

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://avatars.githubusercontent.com/u/123',
    isFrankFrameworkMember: true,
  };

  const mockBuildInfo: BuildInfo = {
    version: '0.0.1',
    time: '2026-01-21T12:00:00Z',
  };

  beforeEach(async () => {
    mockAuthService = {
      currentUser: signal<User | null>(null),
      isAuthenticated: signal<boolean>(false),
      authError: signal<string | null>(null),
      isLoading: signal<boolean>(false),
      setLoading: jasmine.createSpy('setLoading'),
      logout: jasmine.createSpy('logout').and.returnValue(of()),
      clearError: jasmine.createSpy('clearError'),
    };
    mockLocationService = jasmine.createSpyObj('LocationService', ['navigateTo', 'reload']);
    mockVersionService = jasmine.createSpyObj('VersionService', ['getBuildInformation']);
    mockVersionService.getBuildInformation.and.returnValue(of(mockBuildInfo));
    mockGraphStateService = jasmine.createSpyObj('GraphStateService', [
      'getShowExtendedSupport',
      'saveExtendedForOAuth',
      'getGraphQueryParams',
    ]);
    mockGraphStateService.getShowExtendedSupport.and.returnValue(false);
    mockGraphStateService.getGraphQueryParams.and.returnValue({});

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: LocationService, useValue: mockLocationService },
        { provide: VersionService, useValue: mockVersionService },
        { provide: GraphStateService, useValue: mockGraphStateService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  describe('Initial State', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have showUserMenu set to false initially', () => {
      expect(component.showUserMenu).toBe(false);
    });

    it('should inject AuthService', () => {
      expect(component.authService).toBeDefined();
    });
  });

  describe('Navigation Elements', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display the logo and app title', () => {
      const logoContainer = fixture.debugElement.query(By.css('.logo-container'));

      expect(logoContainer).toBeTruthy();

      const img = logoContainer.query(By.css('img'));

      expect(img).toBeTruthy();
      expect(img.nativeElement.getAttribute('alt')).toBe('FF! insights Logo');

      const title = logoContainer.query(By.css('h2'));

      expect(title.nativeElement.textContent).toBe('Insights');
    });

    it('should display build version from VersionService', fakeAsync(() => {
      tick();
      fixture.detectChanges();
      const versionBadge = fixture.debugElement.query(By.css('.build-info'));

      expect(versionBadge).toBeTruthy();
      expect(versionBadge.nativeElement.textContent).toContain('0.0.1');
    }));
  });

  describe('Authentication Section - Not Authenticated', () => {
    beforeEach(() => {
      mockAuthService.isAuthenticated.set(false);
      mockAuthService.currentUser.set(null);
      mockAuthService.isLoading.set(false);
      fixture.detectChanges();
    });

    it('should display login button', () => {
      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton).toBeTruthy();
    });

    it('should not display user profile', () => {
      const userProfile = fixture.debugElement.query(By.css('.user-profile'));

      expect(userProfile).toBeFalsy();
    });

    it('should call onLoginWithGitHub when login button is clicked', () => {
      spyOn(component, 'onLoginWithGitHub');
      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));
      loginButton.nativeElement.click();

      expect(component.onLoginWithGitHub).toHaveBeenCalledWith();
    });

    it('should disable login button when loading', () => {
      mockAuthService.isLoading.set(true);
      fixture.detectChanges();
      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton.nativeElement.disabled).toBe(true);
    });
  });

  describe('Authentication Section - Authenticated', () => {
    beforeEach(() => {
      mockAuthService.isAuthenticated.set(true);
      mockAuthService.currentUser.set(mockUser);
      mockAuthService.isLoading.set(false);
      fixture.detectChanges();
    });

    it('should display user profile', () => {
      const userProfile = fixture.debugElement.query(By.css('.user-profile'));

      expect(userProfile).toBeTruthy();
    });

    it('should not display login button', () => {
      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton).toBeFalsy();
    });

    it('should display user avatar and username', () => {
      const avatar = fixture.debugElement.query(By.css('.user-profile .avatar'));

      expect(avatar).toBeTruthy();
      expect(avatar.nativeElement.src).toBe(mockUser.avatarUrl);

      const username = fixture.debugElement.query(By.css('.user-profile .username'));

      expect(username).toBeTruthy();
      expect(username.nativeElement.textContent).toBe(mockUser.username);
    });

    it('should toggle user menu when clicked', () => {
      const userProfile = fixture.debugElement.query(By.css('.user-profile'));

      // Open
      userProfile.nativeElement.click();
      fixture.detectChanges();

      expect(component.showUserMenu).toBe(true);
      expect(fixture.debugElement.query(By.css('.user-menu'))).toBeTruthy();

      // Close
      userProfile.nativeElement.click();
      fixture.detectChanges();

      expect(component.showUserMenu).toBe(false);
    });

    it('should call onLogout when logout button is clicked', () => {
      spyOn(component, 'onLogout');
      component.showUserMenu = true;
      fixture.detectChanges();

      const logoutButton = fixture.debugElement.query(By.css('.logout-btn'));

      expect(logoutButton).toBeTruthy();
      logoutButton.nativeElement.click();

      expect(component.onLogout).toHaveBeenCalledWith();
    });
  });

  describe('Component Actions', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('onLoginWithGitHub should set loading, save state, and navigate', () => {
      component.onLoginWithGitHub();

      expect(mockAuthService.setLoading).toHaveBeenCalledWith(true);
      expect(mockGraphStateService.getShowExtendedSupport).toHaveBeenCalledWith();
      expect(mockGraphStateService.saveExtendedForOAuth).toHaveBeenCalledWith(false);
      expect(mockLocationService.navigateTo).toHaveBeenCalledWith('/oauth2/authorization/github');
    });

    it('onDismissError should call authService.clearError', () => {
      component.onDismissError();

      expect(mockAuthService.clearError).toHaveBeenCalledWith();
    });

    it('onLogout should close menu and call authService.logout', () => {
      component.showUserMenu = true;
      component.onLogout();

      expect(component.showUserMenu).toBe(false);
      expect(mockAuthService.logout).toHaveBeenCalledWith();
    });
  });
});
