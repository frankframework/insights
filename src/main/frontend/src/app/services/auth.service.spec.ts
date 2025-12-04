import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService, User, ErrorResponse } from './auth.service';
import { AppService } from '../app.service';
import { LocationService } from './location.service';

// to prevent logout and redirection use this test interceptor
class MockHttpInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const requestWithHeaders = request.clone({
      setHeaders: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });
    return next.handle(requestWithHeaders);
  }
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let mockLocationService: jasmine.SpyObj<LocationService>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://avatars.githubusercontent.com/u/123',
    isFrankFrameworkMember: true,
  };

  beforeEach(() => {
    mockLocationService = jasmine.createSpyObj('LocationService', ['navigateTo', 'reload']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        AppService,
        { provide: LocationService, useValue: mockLocationService },
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

    service.currentUser.set(null);
    service.isAuthenticated.set(false);
    service.authError.set(null);
    service.isLoading.set(false);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should have null currentUser initially', () => {
      expect(service.currentUser()).toBeNull();
    });

    it('should have isAuthenticated set to false initially', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should have null authError initially', () => {
      expect(service.authError()).toBeNull();
    });

    it('should have isLoading set to false initially', () => {
      expect(service.isLoading()).toBe(false);
    });
  });

  describe('checkAuthStatus()', () => {
    it('should set isLoading to true when called', () => {
      service.checkAuthStatus().subscribe();

      expect(service.isLoading()).toBe(true);

      const request = httpMock.expectOne('/api/auth/user');
      request.flush(mockUser);
    });

    it('should successfully authenticate and set user data on 200 response', (done) => {
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toEqual(mockUser);
        expect(service.currentUser()).toEqual(mockUser);
        expect(service.isAuthenticated()).toBe(true);
        expect(service.authError()).toBeNull();
        expect(service.isLoading()).toBe(false);
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');

      expect(request.request.method).toBe('GET');
      expect(request.request.withCredentials).toBe(true);
      request.flush(mockUser);
    });

    it('should handle 401 (not authenticated) gracefully without setting error', (done) => {
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        expect(service.authError()).toBeNull();
        expect(service.isLoading()).toBe(false);
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush(null, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 403 (forbidden) and set error message from backend', (done) => {
      const errorResponse: ErrorResponse = {
        httpStatus: 403,
        messages: ['You are not a member of the frankframework organization.'],
        errorCode: 'FORBIDDEN',
      };

      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        expect(service.authError()).toBe('You are not a member of the frankframework organization.');
        expect(service.isLoading()).toBe(false);
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush(errorResponse, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 403 with multiple error messages', (done) => {
      const errorResponse: ErrorResponse = {
        httpStatus: 403,
        messages: ['Error 1', 'Error 2', 'Error 3'],
        errorCode: 'FORBIDDEN',
      };

      service.checkAuthStatus().subscribe(() => {
        expect(service.authError()).toBe('Error 1 Error 2 Error 3');
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush(errorResponse, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 403 without error messages with default message', (done) => {
      service.checkAuthStatus().subscribe(() => {
        expect(service.authError()).toBe('Access denied. You must be a member of the frankframework organization.');
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush({}, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 403 with empty messages array with default message', (done) => {
      const errorResponse: ErrorResponse = {
        httpStatus: 403,
        messages: [],
        errorCode: 'FORBIDDEN',
      };

      service.checkAuthStatus().subscribe(() => {
        expect(service.authError()).toBe('Access denied. You must be a member of the frankframework organization.');
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush(errorResponse, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle other HTTP errors (e.g., 500) without setting error message', (done) => {
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        expect(service.authError()).toBeNull();
        expect(service.isLoading()).toBe(false);
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle network errors', (done) => {
      service.checkAuthStatus().subscribe((user) => {
        expect(user).toBeNull();
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        expect(service.authError()).toBeNull();
        expect(service.isLoading()).toBe(false);
        done();
      });

      const request = httpMock.expectOne('/api/auth/user');
      request.error(new ErrorEvent('Network error'));
    });
  });

  describe('clearError()', () => {
    it('should clear the authError signal', () => {
      service.authError.set('Some error message');

      service.clearError();

      expect(service.authError()).toBeNull();
    });

    it('should work when authError is already null', () => {
      service.authError.set(null);

      service.clearError();

      expect(service.authError()).toBeNull();
    });
  });

  describe('setLoading()', () => {
    it('should set isLoading to true', () => {
      service.setLoading(true);

      expect(service.isLoading()).toBe(true);
    });

    it('should set isLoading to false', () => {
      service.isLoading.set(true);

      service.setLoading(false);

      expect(service.isLoading()).toBe(false);
    });
  });

  describe('logout()', () => {
    it('should successfully logout and clear auth state', (done) => {
      service.currentUser.set(mockUser);
      service.isAuthenticated.set(true);
      service.authError.set('Some error');

      service.logout().subscribe(() => {
        expect(service.isLoading()).toBe(false);
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
        expect(service.authError()).toBeNull();
        done();
      });

      const request = httpMock.expectOne('/api/auth/logout');

      expect(request.request.method).toBe('POST');
      expect(request.request.withCredentials).toBe(true);
      request.flush(null);
    });
  });
});
