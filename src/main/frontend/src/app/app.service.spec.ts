import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppService } from './app.service';
import { environment } from '../environments/environment';
import { provideHttpClient } from '@angular/common/http';

describe('AppService', () => {
  let service: AppService;
  let httpTestingController: HttpTestingController;

  const originalBackendUrl = environment.backendUrl;
  const MOCK_API_URL = 'http://localhost:8080/api';

  beforeEach(() => {
    environment.backendUrl = MOCK_API_URL;

    TestBed.configureTestingModule({
      providers: [AppService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AppService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    environment.backendUrl = originalBackendUrl;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('get()', () => {
    const testUrl = 'test-endpoint';
    const mockData = { message: 'success' };

    it('should make a GET request with no parameters', () => {
      service.get(testUrl).subscribe((data) => {
        expect(data).toEqual(mockData);
      });

      const request = httpTestingController.expectOne(testUrl);

      expect(request.request.method).toBe('GET');
      expect(request.request.params.keys().length).toBe(0);

      request.flush(mockData);
    });

    it('should make a GET request with correct string and number parameters', () => {
      const parameters = { name: 'frank', version: 8 };

      service.get(testUrl, parameters).subscribe();

      const request = httpTestingController.expectOne((r) => r.url === testUrl);

      expect(request.request.method).toBe('GET');

      expect(request.request.params.get('name')).toBe('frank');
      expect(request.request.params.get('version')).toBe('8');

      request.flush(mockData);
    });

    it('should filter out null, undefined, and empty string parameters', () => {
      const parameters = {
        name: 'frank',
        status: null,
        branch: undefined,
        tag: '',
      };

      service.get(testUrl, parameters as any).subscribe();

      const request = httpTestingController.expectOne((r) => r.url === testUrl);

      expect(request.request.params.keys().length).toBe(1);
      expect(request.request.params.has('name')).toBe(true);
      expect(request.request.params.has('status')).toBe(false);
      expect(request.request.params.has('branch')).toBe(false);
      expect(request.request.params.has('tag')).toBe(false);

      request.flush(mockData);
    });

    it('should convert a complex object parameter to "[object Object]"', () => {
      const parameters = { time: { start: 'now', end: 'later' } as any };

      service.get(testUrl, parameters).subscribe();

      const request = httpTestingController.expectOne((r) => r.url === testUrl);

      expect(request.request.params.get('time')).toBe('[object Object]');

      request.flush(mockData);
    });
  });

  describe('createAPIUrl()', () => {
    let actualBaseUrl: string;

    beforeEach(() => {
      actualBaseUrl = (service as any).constructor.API_BASE_URL;
    });

    it('should correctly construct an API URL from the environment', () => {
      const endpoint = 'releases';
      const expectedUrl = `${actualBaseUrl}/releases`;

      expect(service.createAPIUrl(endpoint)).toBe(expectedUrl);
    });

    it('should construct a URL even if the endpoint is empty', () => {
      const endpoint = '';
      const expectedUrl = `${actualBaseUrl}/`;

      expect(service.createAPIUrl(endpoint)).toBe(expectedUrl);
    });

    it('should create a double slash if the endpoint starts with a slash (current behavior)', () => {
      const endpoint = '/users';
      const expectedUrl = `${actualBaseUrl}//users`;

      expect(service.createAPIUrl(endpoint)).toBe(expectedUrl);
    });
  });
});
