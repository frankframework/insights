import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BusinessValueService, BusinessValue } from './business-value.service';
import { AppService } from '../app.service';

describe('BusinessValueService', () => {
  let service: BusinessValueService;
  let httpMock: HttpTestingController;

  const mockBusinessValue: BusinessValue = {
    id: '123',
    title: 'Enhance Performance',
    description: 'Improve app load times',
    releaseId: 'release-1',
    issues: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BusinessValueService,
        AppService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(BusinessValueService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getBusinessValuesByReleaseId', () => {
    it('should retrieve business values for a release via GET', () => {
      const releaseId = 'release-1';
      const mockResponse: BusinessValue[] = [mockBusinessValue];

      service.getBusinessValuesByReleaseId(releaseId).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(1);
      });

      const request = httpMock.expectOne(`/api/business-value/release/${releaseId}`);

      expect(request.request.method).toBe('GET');
      request.flush(mockResponse);
    });
  });

  describe('getBusinessValueById', () => {
    it('should retrieve a single business value by ID via GET', () => {
      service.getBusinessValueById('123').subscribe((response) => {
        expect(response).toEqual(mockBusinessValue);
      });

      const request = httpMock.expectOne('/api/business-value/123');

      expect(request.request.method).toBe('GET');
      request.flush(mockBusinessValue);
    });
  });

  describe('createBusinessValue', () => {
    it('should create a business value via POST with releaseId', () => {
      const title = 'New Value';
      const description = 'Description';
      const releaseId = 'release-1';

      service.createBusinessValue(title, description, releaseId).subscribe((response) => {
        expect(response).toEqual(mockBusinessValue);
      });

      const request = httpMock.expectOne('/api/business-value');

      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ title, description, releaseId });
      request.flush(mockBusinessValue);
    });
  });

  describe('updateBusinessValue', () => {
    it('should update a business value via PUT without releaseId', () => {
      const id = '123';
      const title = 'Updated Title';
      const description = 'Updated Description';

      service.updateBusinessValue(id, title, description).subscribe((response) => {
        expect(response).toEqual(mockBusinessValue);
      });

      const request = httpMock.expectOne(`/api/business-value/${id}`);

      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({ title, description });
      request.flush(mockBusinessValue);
    });
  });

  describe('updateIssueConnections', () => {
    it('should update issue connections via PUT', () => {
      const id = '123';
      const issueIds = ['ISSUE-1', 'ISSUE-2'];

      service.updateIssueConnections(id, issueIds).subscribe((response) => {
        expect(response).toEqual(mockBusinessValue);
      });

      const request = httpMock.expectOne(`/api/business-value/${id}/issues`);

      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({ issueIds });
      request.flush(mockBusinessValue);
    });
  });

  describe('deleteBusinessValue', () => {
    it('should delete a business value via DELETE', () => {
      const id = '123';

      service.deleteBusinessValue(id).subscribe((response) => {
        expect(response).toBeNull();
      });

      const request = httpMock.expectOne(`/api/business-value/${id}`);

      expect(request.request.method).toBe('DELETE');
      request.flush(null);
    });
  });

  describe('duplicateBusinessValues', () => {
    it('should duplicate business values via POST', () => {
      const targetReleaseId = 'release-2';
      const sourceReleaseId = 'release-1';
      const mockResponse: BusinessValue[] = [mockBusinessValue];

      service.duplicateBusinessValues(targetReleaseId, sourceReleaseId).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(1);
      });

      const request = httpMock.expectOne(`/api/business-value/release/${targetReleaseId}/duplicate`);

      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ sourceReleaseId });
      request.flush(mockResponse);
    });
  });
});
