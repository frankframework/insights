import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService, User, ErrorResponse } from './auth.service';
import { AppService } from '../app.service';
import { LocationService } from './location.service';
import { GraphStateService } from './graph-state.service';

class MockHttpInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const requestWithHeaders = request.clone({
      setHeaders: { 'Content-Type': 'application/json' },
      withCredentials: true,
    });
    return next.handle(requestWithHeaders);
  }
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let mockLocationService: jasmine.SpyObj<LocationService>;
  let mockGraphStateService: jasmine.SpyObj<GraphStateService>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://avatars.githubusercontent.com/u/123',
    isFrankFrameworkMember: true,
  };

  beforeEach(() => {
    mockLocationService = jasmine.createSpyObj('LocationService', ['navigateTo', 'reload']);
    mockGraphStateService = jasmine.createSpyObj('GraphStateService', ['getGraphQueryParams']);
    mockGraphStateService.getGraphQueryParams.and.returnValue({});

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        AppService,
        { provide: LocationService, useValue: mockLocationService },
        { provide: GraphStateService, useValue: mockGraphStateService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: MockHttpInterceptor,
          multi: true,
        },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Reset signals and Storage
    service.currentUser.set(null);
    service.isAuthenticated.set(false);
    service.authError.set(null);
    service.isLoading.set(false);
    // eslint-disable-next-line no-undef
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('checkAuthStatus()', () => {
    it('should return null immediately if no session exists in localStorage', (done) => {
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        done();
      });
    });

    it('should set isLoading to true when called with valid session', () => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_session', 'true');
      service.checkAuthStatus().subscribe();

      expect(service.isLoading()).toBe(true);

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/user'));
      request.flush(mockUser);
    });

    it('should successfully authenticate and set user data on 200 response', (done) => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_session', 'true');

      service.checkAuthStatus().subscribe((user) => {
        expect(user).toEqual(mockUser);
        expect(service.currentUser()).toEqual(mockUser);
        expect(service.isAuthenticated()).toBe(true);
        // eslint-disable-next-line no-undef
        expect(localStorage.getItem('auth_session')).toBe('true');
        done();
      });

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/user'));
      request.flush(mockUser);
    });

    it('should handle 401 and clear session flag', (done) => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_session', 'true');

      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        // eslint-disable-next-line no-undef
        expect(localStorage.getItem('auth_session')).toBeNull();
        expect(service.authError()).toBeNull();
        done();
      });

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/user'));
      request.flush(null, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 403 and set error message', (done) => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_session', 'true');
      const errorResponse: ErrorResponse = {
        httpStatus: 403,
        messages: ['Not a member'],
        errorCode: 'FORBIDDEN',
      };

      service.checkAuthStatus().subscribe(() => {
        expect(service.authError()).toBe('Not a member');
        // eslint-disable-next-line no-undef
        expect(localStorage.getItem('auth_session')).toBeNull();
        done();
      });

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/user'));
      request.flush(errorResponse, { status: 403, statusText: 'Forbidden' });
    });
  });

  describe('setPendingAuth()', () => {
    it('should set session flag in localStorage', () => {
      service.setPendingAuth();

      // eslint-disable-next-line no-undef
      expect(localStorage.getItem('auth_session')).toBe('true');
    });

    it('should allow checkAuthStatus to make backend call after setPendingAuth', (done) => {
      service.setPendingAuth();
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toEqual(mockUser);
        done();
      });

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/user'));
      request.flush(mockUser);
    });
  });

  describe('logout()', () => {
    it('should successfully logout and clear localStorage', (done) => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_session', 'true');
      service.isAuthenticated.set(true);

      service.logout().subscribe(() => {
        expect(service.isAuthenticated()).toBe(false);
        // eslint-disable-next-line no-undef
        expect(localStorage.getItem('auth_session')).toBeNull();
        expect(mockLocationService.navigateTo).toHaveBeenCalledWith('/');
        done();
      });

      const request = httpMock.expectOne((request_) => request_.url.includes('auth/logout'));
      request.flush(null);
    });
  });
});
