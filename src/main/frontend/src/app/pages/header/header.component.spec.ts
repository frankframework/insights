import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { AuthService, User } from '../../services/auth.service';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { LocationService } from '../../services/location.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let mockAuthService: any;
  let mockLocationService: jasmine.SpyObj<LocationService>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://avatars.githubusercontent.com/u/123',
    isFrankFrameworkMember: true,
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

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: LocationService, useValue: mockLocationService },
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should have showUserMenu set to false initially', () => {
      expect(component.showUserMenu).toBe(false);
    });

    it('should inject AuthService', () => {
      expect(component.authService).toBeDefined();
    });
  });

  describe('Navigation Elements', () => {
    it('should display the logo and app title', () => {
      fixture.detectChanges();

      const logoContainer = fixture.debugElement.query(By.css('.logo-container'));

      expect(logoContainer).toBeTruthy();

      const img = logoContainer.query(By.css('img'));

      expect(img).toBeTruthy();
      expect(img.nativeElement.getAttribute('alt')).toBe('FF! insights Logo');

      const title = logoContainer.query(By.css('h2'));

      expect(title.nativeElement.textContent).toBe('Insights');
    });

    it('should display Release graph navigation link', () => {
      fixture.detectChanges();

      const links = fixture.debugElement.queryAll(By.css('li[routerLink]'));
      const graphLink = links.find((link) => link.nativeElement.textContent.includes('Release graph'));

      expect(graphLink).toBeTruthy();
      expect(graphLink?.attributes['routerLink']).toBe('/graph');
    });

    it('should display Roadmap navigation link', () => {
      fixture.detectChanges();

      const links = fixture.debugElement.queryAll(By.css('li[routerLink]'));
      const roadmapLink = links.find((link) => link.nativeElement.textContent.includes('Roadmap'));

      expect(roadmapLink).toBeTruthy();
      expect(roadmapLink?.attributes['routerLink']).toBe('/roadmap');
    });
  });

  describe('Authentication Section - Not Authenticated', () => {
    beforeEach(() => {
      mockAuthService.isAuthenticated.set(false);
      mockAuthService.currentUser.set(null);
      mockAuthService.isLoading.set(false);
    });

    it('should display login button when not authenticated', () => {
      fixture.detectChanges();

      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton).toBeTruthy();
    });

    it('should not display user profile when not authenticated', () => {
      fixture.detectChanges();

      const userProfile = fixture.debugElement.query(By.css('.user-profile'));

      expect(userProfile).toBeFalsy();
    });

    it('should call onLoginWithGitHub when login button is clicked', () => {
      spyOn(component, 'onLoginWithGitHub');
      fixture.detectChanges();

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

    it('should not disable login button when not loading', () => {
      mockAuthService.isLoading.set(false);
      fixture.detectChanges();

      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton.nativeElement.disabled).toBe(false);
    });

    it('should show spinner when loading', () => {
      mockAuthService.isLoading.set(true);
      fixture.detectChanges();

      const spinner = fixture.debugElement.query(By.css('.spinner'));

      expect(spinner).toBeTruthy();
    });

    it('should show GitHub logo when not loading', () => {
      mockAuthService.isLoading.set(false);
      fixture.detectChanges();

      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));
      const svgs = loginButton.queryAll(By.css('svg'));
      const githubIcon = svgs.find((svg) => !svg.nativeElement.classList.contains('spinner'));

      expect(githubIcon).toBeTruthy();
    });
  });

  describe('Authentication Section - Authenticated', () => {
    beforeEach(() => {
      mockAuthService.isAuthenticated.set(true);
      mockAuthService.currentUser.set(mockUser);
      mockAuthService.isLoading.set(false);
    });

    it('should display user profile when authenticated', () => {
      fixture.detectChanges();

      const userProfile = fixture.debugElement.query(By.css('.user-profile'));

      expect(userProfile).toBeTruthy();
    });

    it('should not display login button when authenticated', () => {
      fixture.detectChanges();

      const loginButton = fixture.debugElement.query(By.css('.github-login-btn'));

      expect(loginButton).toBeFalsy();
    });

    it('should display user avatar in profile', () => {
      fixture.detectChanges();

      const avatar = fixture.debugElement.query(By.css('.user-profile .avatar'));

      expect(avatar).toBeTruthy();
      expect(avatar.nativeElement.src).toBe(mockUser.avatarUrl);
      expect(avatar.nativeElement.alt).toBe(mockUser.username);
    });

    it('should display username in profile', () => {
      fixture.detectChanges();

      const username = fixture.debugElement.query(By.css('.user-profile .username'));

      expect(username).toBeTruthy();
      expect(username.nativeElement.textContent).toBe(mockUser.username);
    });

    it('should call toggleUserMenu when profile is clicked', () => {
      spyOn(component, 'toggleUserMenu');
      fixture.detectChanges();

      const userProfile = fixture.debugElement.query(By.css('.user-profile'));
      userProfile.nativeElement.click();

      expect(component.toggleUserMenu).toHaveBeenCalledWith();
    });

    it('should not show user menu by default', () => {
      component.showUserMenu = false;
      fixture.detectChanges();

      const userMenu = fixture.debugElement.query(By.css('.user-menu'));

      expect(userMenu).toBeFalsy();
    });

    it('should show user menu when showUserMenu is true', () => {
      component.showUserMenu = true;
      fixture.detectChanges();

      const userMenu = fixture.debugElement.query(By.css('.user-menu'));

      expect(userMenu).toBeTruthy();
    });

    it('should display user details in menu', () => {
      component.showUserMenu = true;
      fixture.detectChanges();

      const usernameLarge = fixture.debugElement.query(By.css('.username-large'));

      expect(usernameLarge.nativeElement.textContent).toBe(mockUser.username);

      const userId = fixture.debugElement.query(By.css('.user-id'));

      expect(userId.nativeElement.textContent).toContain(mockUser.githubId.toString());
    });

    it('should display logout button in menu', () => {
      component.showUserMenu = true;
      fixture.detectChanges();

      const logoutButton = fixture.debugElement.query(By.css('.logout-btn'));

      expect(logoutButton).toBeTruthy();
      expect(logoutButton.nativeElement.textContent.trim()).toContain('Logout');
    });

    it('should call onLogout when logout button is clicked', () => {
      spyOn(component, 'onLogout');
      component.showUserMenu = true;
      fixture.detectChanges();

      const logoutButton = fixture.debugElement.query(By.css('.logout-btn'));
      logoutButton.nativeElement.click();

      expect(component.onLogout).toHaveBeenCalledWith();
    });

    it('should respond to Enter key on user profile', () => {
      spyOn(component, 'toggleUserMenu');
      fixture.detectChanges();

      const userProfile = fixture.debugElement.query(By.css('.user-profile'));
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      userProfile.nativeElement.dispatchEvent(event);

      expect(component.toggleUserMenu).toHaveBeenCalledWith();
    });

    it('should respond to Space key on user profile', () => {
      spyOn(component, 'toggleUserMenu');
      fixture.detectChanges();

      const userProfile = fixture.debugElement.query(By.css('.user-profile'));
      const event = new KeyboardEvent('keydown', { key: ' ' });
      userProfile.nativeElement.dispatchEvent(event);

      expect(component.toggleUserMenu).toHaveBeenCalledWith();
    });
  });

  describe('Error Banner', () => {
    it('should not display error banner when authError is null', () => {
      mockAuthService.authError.set(null);
      fixture.detectChanges();

      const errorBanner = fixture.debugElement.query(By.css('.auth-error-banner'));

      expect(errorBanner).toBeFalsy();
    });

    it('should display error banner when authError has a message', () => {
      mockAuthService.authError.set('Test error message');
      fixture.detectChanges();

      const errorBanner = fixture.debugElement.query(By.css('.auth-error-banner'));

      expect(errorBanner).toBeTruthy();
    });

    it('should display the error message', () => {
      const errorMessage = 'Test error message';
      mockAuthService.authError.set(errorMessage);
      fixture.detectChanges();

      const errorMessageElement = fixture.debugElement.query(By.css('.error-message'));

      expect(errorMessageElement.nativeElement.textContent).toBe(errorMessage);
    });

    it('should display dismiss button', () => {
      mockAuthService.authError.set('Test error');
      fixture.detectChanges();

      const dismissButton = fixture.debugElement.query(By.css('.dismiss-btn'));

      expect(dismissButton).toBeTruthy();
    });

    it('should call onDismissError when dismiss button is clicked', () => {
      spyOn(component, 'onDismissError');
      mockAuthService.authError.set('Test error');
      fixture.detectChanges();

      const dismissButton = fixture.debugElement.query(By.css('.dismiss-btn'));
      dismissButton.nativeElement.click();

      expect(component.onDismissError).toHaveBeenCalledWith();
    });
  });

  describe('onLoginWithGitHub()', () => {
    it('should set loading to true and redirect to GitHub OAuth', () => {
      component.onLoginWithGitHub();

      expect(mockAuthService.setLoading).toHaveBeenCalledWith(true);
    });
  });

  describe('toggleUserMenu()', () => {
    it('should toggle showUserMenu from false to true', () => {
      component.showUserMenu = false;

      component.toggleUserMenu();

      expect(component.showUserMenu).toBe(true);
    });

    it('should toggle showUserMenu from true to false', () => {
      component.showUserMenu = true;

      component.toggleUserMenu();

      expect(component.showUserMenu).toBe(false);
    });
  });

  describe('closeUserMenu()', () => {
    it('should set showUserMenu to false', () => {
      component.showUserMenu = true;

      component.closeUserMenu();

      expect(component.showUserMenu).toBe(false);
    });

    it('should keep showUserMenu false when already false', () => {
      component.showUserMenu = false;

      component.closeUserMenu();

      expect(component.showUserMenu).toBe(false);
    });
  });

  describe('onLogout()', () => {
    it('should close the user menu', () => {
      component.showUserMenu = true;

      component.onLogout();

      expect(component.showUserMenu).toBe(false);
    });

    it('should call authService.logout()', () => {
      component.onLogout();

      expect(mockAuthService.logout).toHaveBeenCalledWith();
    });
  });

  describe('onDismissError()', () => {
    it('should call authService.clearError()', () => {
      component.onDismissError();

      expect(mockAuthService.clearError).toHaveBeenCalledWith();
    });
  });
});
