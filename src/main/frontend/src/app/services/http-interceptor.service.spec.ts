import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { HttpInterceptorService } from './http-interceptor.service';
import { AuthService } from './auth.service';

describe('HttpInterceptorService', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['logout']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        HttpInterceptorService,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: HTTP_INTERCEPTORS,
          useClass: HttpInterceptorService,
          multi: true,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    authService.logout.and.returnValue(of());
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should logout and redirect on 401 Unauthorized error', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    tick();

    expect(authService.logout).toHaveBeenCalledWith();
    expect(router.navigate).toHaveBeenCalledWith(['/graph']);
  }));

  it('should logout and redirect on 403 Forbidden error', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    tick();

    expect(authService.logout).toHaveBeenCalledWith();
    expect(router.navigate).toHaveBeenCalledWith(['/graph']);
  }));

  it('should NOT logout on 500 Internal Server Error', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Something went wrong' }, { status: 500, statusText: 'Internal Server Error' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should NOT logout on 400 error (even with "invalid token" message)', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Invalid token provided' }, { status: 400, statusText: 'Bad Request' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should NOT logout on other errors like 404', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should NOT logout on 429 rate limiting error', fakeAsync(() => {
    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Too many requests' }, { status: 429, statusText: 'Too Many Requests' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should NOT logout if request is to /auth/logout endpoint', fakeAsync(() => {
    httpClient.post('/api/auth/logout', {}).subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/auth/logout');
    request.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should NOT logout if request is to /auth/user endpoint', fakeAsync(() => {
    httpClient.get('/api/auth/user').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/auth/user');
    request.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    tick();

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('should navigate to login even if logout fails', fakeAsync(() => {
    authService.logout.and.returnValue(throwError(() => new Error('Logout failed')));

    httpClient.get('/api/data').subscribe({
      error: () => {}
    });

    const request = httpTestingController.expectOne('/api/data');
    request.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    tick();

    expect(authService.logout).toHaveBeenCalledWith();
    expect(router.navigate).toHaveBeenCalledWith(['/graph']);
  }));
});
