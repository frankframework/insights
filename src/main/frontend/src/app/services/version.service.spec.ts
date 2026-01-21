import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { VersionService, ActuatorInfo } from './version.service';

describe('VersionService', () => {
  let service: VersionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        VersionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(VersionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return the version string when API call is successful', () => {
    const mockResponse: ActuatorInfo = {
      build: {
        version: '1.2.3',
        time: '2024-01-01T12:00:00Z'
      }
    };

    service.getBuildInformation().subscribe(version => {
      expect(version?.version).toBe('1.2.3');
    });

    const request = httpMock.expectOne('/actuator/info');

    expect(request.request.method).toBe('GET');
    request.flush(mockResponse);
  });

  it('should return null when API returns empty or malformed data', () => {
    const emptyResponse = {};

    service.getBuildInformation().subscribe(version => {
      expect(version).toBe(null);
    });

    const request = httpMock.expectOne('/actuator/info');
    request.flush(emptyResponse);
  });

  it('should return null" when the API fails (404/500)', () => {
    service.getBuildInformation().subscribe(version => {
      expect(version).toBe(null);
    });

    const request = httpMock.expectOne('/actuator/info');
    request.flush('Something went wrong', { status: 500, statusText: 'Server Error' });
  });
});
