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
      service.get(testUrl).subscribe(data => {
        expect(data).toEqual(mockData);
      });

      const req = httpTestingController.expectOne(testUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);

      req.flush(mockData);
    });

    it('should make a GET request with correct string and number parameters', () => {
      const parameters = { name: 'frank', version: 8 };

      service.get(testUrl, parameters).subscribe();

      const req = httpTestingController.expectOne(r => r.url === testUrl);
      expect(req.request.method).toBe('GET');

      expect(req.request.params.get('name')).toBe('frank');
      expect(req.request.params.get('version')).toBe('8');

      req.flush(mockData);
    });

    it('should filter out null, undefined, and empty string parameters', () => {
      const parameters = {
        name: 'frank',
        status: null,
        branch: undefined,
        tag: '',
      };

      service.get(testUrl, parameters as any).subscribe();

      const req = httpTestingController.expectOne(r => r.url === testUrl);

      expect(req.request.params.keys().length).toBe(1);
      expect(req.request.params.has('name')).toBe(true);
      expect(req.request.params.has('status')).toBe(false);
      expect(req.request.params.has('branch')).toBe(false);
      expect(req.request.params.has('tag')).toBe(false);

      req.flush(mockData);
    });

    it('should convert a complex object parameter to "[object Object]"', () => {
      const parameters = { time: { start: 'now', end: 'later' } as any };

      service.get(testUrl, parameters).subscribe();

      const req = httpTestingController.expectOne(r => r.url === testUrl);
      expect(req.request.params.get('time')).toBe('[object Object]');

      req.flush(mockData);
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

  describe('isValidISODate()', () => {
    it('should return true for a valid ISO 8601 date string with time', () => {
      const validDate = '2025-06-15T16:59:59';
      expect(service.isValidISODate(validDate)).toBe(true);
    });

    it('should return true for a valid ISO 8601 date string with Z-suffix', () => {
      const validDate = '2025-06-15T16:59:59Z';
      expect(service.isValidISODate(validDate)).toBe(true);
    });

    it('should return false for a date string without the "T" separator', () => {
      const invalidDate = '2025-06-15 16:59:59';
      expect(service.isValidISODate(invalidDate)).toBe(false);
    });

    it('should return false for a date-only string', () => {
      const invalidDate = '2025-06-15';
      expect(service.isValidISODate(invalidDate)).toBe(false);
    });

    it('should return false for a non-standard date format', () => {
      const invalidDate = '15-06-2025';
      expect(service.isValidISODate(invalidDate)).toBe(false);
    });

    it('should return false for an empty string', () => {
      expect(service.isValidISODate('')).toBe(false);
    });

    it('should return false for a random string', () => {
      expect(service.isValidISODate('hello world')).toBe(false);
    });
  });
});
