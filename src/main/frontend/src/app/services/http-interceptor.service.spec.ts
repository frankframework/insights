import { TestBed } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { HttpInterceptorService } from './http-interceptor.service';

describe('HttpInterceptorService', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        HttpInterceptorService,
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
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should add Content-Type header and withCredentials flag to requests', () => {
    httpClient.get('/api/data').subscribe();

    const request = httpTestingController.expectOne('/api/data');

    expect(request.request.headers.get('Content-Type')).toBe('application/json');
    expect(request.request.withCredentials).toBeTrue();

    request.flush({});
  });

  describe('CSRF Token Handling', () => {
    it('should add X-XSRF-TOKEN header when token cookie exists and method is POST', () => {
      spyOnProperty(document, 'cookie', 'get').and.returnValue('XSRF-TOKEN=abc-123-def');

      httpClient.post('/api/create', {}).subscribe();

      const request = httpTestingController.expectOne('/api/create');

      expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('abc-123-def');
      request.flush({});
    });

    it('should add X-XSRF-TOKEN header when token cookie exists and method is DELETE', () => {
      spyOnProperty(document, 'cookie', 'get').and.returnValue('XSRF-TOKEN=secure-token-value');

      httpClient.delete('/api/delete/1').subscribe();

      const request = httpTestingController.expectOne('/api/delete/1');

      expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('secure-token-value');
      request.flush({});
    });

    it('should NOT add X-XSRF-TOKEN header for GET requests even if cookie exists', () => {
      spyOnProperty(document, 'cookie', 'get').and.returnValue('XSRF-TOKEN=abc-123');

      httpClient.get('/api/read').subscribe();

      const request = httpTestingController.expectOne('/api/read');

      expect(request.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
      request.flush({});
    });

    it('should warn when method is POST but no CSRF token cookie exists', () => {
      spyOnProperty(document, 'cookie', 'get').and.returnValue('');

      httpClient.post('/api/create', {}).subscribe();

      const request = httpTestingController.expectOne('/api/create');

      expect(request.request.headers.has('X-XSRF-TOKEN')).toBeFalse();

      request.flush({});
    });

    it('should parse CSRF token correctly when multiple cookies exist', () => {
      spyOnProperty(document, 'cookie', 'get').and.returnValue('theme=dark; XSRF-TOKEN=real-token; other=123');

      httpClient.put('/api/update', {}).subscribe();

      const request = httpTestingController.expectOne('/api/update');

      expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('real-token');
      request.flush({});
    });
  });
});
