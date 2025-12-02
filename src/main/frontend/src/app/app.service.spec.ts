import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppService } from './app.service';
import { provideHttpClient } from '@angular/common/http';

describe('AppService', () => {
  let service: AppService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AppService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AppService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('get()', () => {
    const testUrl = 'test-endpoint';
    const mockData = { message: 'success' };

    it('should make a GET request with parameters', () => {
      const parameters = { name: 'frank', version: 8 };

      service.get(testUrl, parameters).subscribe((data) => {
        expect(data).toEqual(mockData);
      });

      const request = httpMock.expectOne((r) => r.url === testUrl);

      expect(request.request.method).toBe('GET');
      expect(request.request.params.get('name')).toBe('frank');
      expect(request.request.params.get('version')).toBe('8');
      request.flush(mockData);
    });
  });

  describe('post()', () => {
    const testUrl = 'test-endpoint';
    const mockBody = { data: 'test' };
    const mockResponse = { id: 1 };

    it('should make a POST request', () => {
      service.post(testUrl, mockBody).subscribe((data) => {
        expect(data).toEqual(mockResponse);
      });

      const request = httpMock.expectOne(testUrl);

      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual(mockBody);
      request.flush(mockResponse);
    });

    it('should support custom headers', () => {
      const headers = { 'X-Custom': 'Value' };
      service.post(testUrl, mockBody, headers).subscribe();

      const request = httpMock.expectOne(testUrl);

      expect(request.request.headers.get('X-Custom')).toBe('Value');
      request.flush(mockResponse);
    });
  });

  describe('put()', () => {
    const testUrl = 'test-endpoint/1';
    const mockBody = { data: 'updated' };
    const mockResponse = { id: 1, data: 'updated' };

    it('should make a PUT request', () => {
      service.put(testUrl, mockBody).subscribe((data) => {
        expect(data).toEqual(mockResponse);
      });

      const request = httpMock.expectOne(testUrl);

      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual(mockBody);
      request.flush(mockResponse);
    });
  });

  describe('delete()', () => {
    const testUrl = 'test-endpoint/1';

    it('should make a DELETE request', () => {
      service.delete(testUrl).subscribe((data) => {
        expect(data).toBeNull();
      });

      const request = httpMock.expectOne(testUrl);

      expect(request.request.method).toBe('DELETE');
      request.flush(null);
    });

    it('should support custom headers in DELETE', () => {
      const headers = { 'X-Auth': 'Token' };
      service.delete(testUrl, headers).subscribe();

      const request = httpMock.expectOne(testUrl);

      expect(request.request.method).toBe('DELETE');
      expect(request.request.headers.get('X-Auth')).toBe('Token');
      request.flush(null);
    });
  });

  describe('createAPIUrl()', () => {
    it('should correctly construct an API URL', () => {
      expect(service.createAPIUrl('users')).toBe('/api/users');
    });
  });
});
