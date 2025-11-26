import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseBusinessValueComponent } from './release-business-value.component';
import { BusinessValueService } from '../../../services/business-value.service';
import { of, throwError } from 'rxjs';

describe('ReleaseBusinessValueComponent', () => {
  let component: ReleaseBusinessValueComponent;
  let fixture: ComponentFixture<ReleaseBusinessValueComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;
  let consoleErrorSpy: jasmine.Spy;

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['getBusinessValuesByReleaseId']);
    consoleErrorSpy = spyOn(globalThis.console, 'error');

    await TestBed.configureTestingModule({
      imports: [ReleaseBusinessValueComponent],
      providers: [{ provide: BusinessValueService, useValue: mockBusinessValueService }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseBusinessValueComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges', () => {
    it('should fetch business values when releaseId changes', () => {
      const mockBusinessValues = [
        { id: '1', title: 'Test Value', description: 'Test Description', issueUrl: 'http://test.com', issueNumber: 123 },
      ];
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(mockBusinessValueService.getBusinessValuesByReleaseId).toHaveBeenCalledWith('test-release-id');
      expect(component.businessValues()).toEqual(mockBusinessValues);
    });

    it('should not fetch business values when releaseId is undefined', () => {
      component.releaseId = undefined;
      component.ngOnChanges({
        releaseId: {
          currentValue: undefined,
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(mockBusinessValueService.getBusinessValuesByReleaseId).not.toHaveBeenCalled();
    });

    it('should not fetch business values when releaseId change is not present', () => {
      component.ngOnChanges({});

      expect(mockBusinessValueService.getBusinessValuesByReleaseId).not.toHaveBeenCalled();
    });
  });

  describe('Loading state', () => {
    it('should set isLoadingBusinessValues to true when fetching and false when complete', () => {
      const mockBusinessValues = [
        { id: '1', title: 'Test Value', description: 'Test Description', issueUrl: 'http://test.com', issueNumber: 123 },
      ];
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      expect(component.isLoadingBusinessValues()).toBe(false);

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.isLoadingBusinessValues()).toBe(false);
      expect(component.businessValues()).toEqual(mockBusinessValues);
    });

    it('should set isLoadingBusinessValues to false even when an error occurs', () => {
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(throwError(() => new Error('Test error')));

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.isLoadingBusinessValues()).toBe(false);
    });
  });

  describe('Error handling', () => {
    it('should handle errors when fetching business values', () => {
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(throwError(() => new Error('Test error')));

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.businessValues()).toEqual([]);
      expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load business values:', jasmine.any(Error));
    });

    it('should return empty array when service returns error', () => {
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(
        throwError(() => new Error('Network error')),
      );

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.businessValues()).toEqual([]);
      expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to load business values:', jasmine.any(Error));
    });
  });

  describe('Multiple business values', () => {
    it('should handle multiple business values', () => {
      const mockBusinessValues = [
        { id: '1', title: 'Value 1', description: 'Description 1', issueUrl: 'http://test1.com', issueNumber: 123 },
        { id: '2', title: 'Value 2', description: 'Description 2', issueUrl: 'http://test2.com', issueNumber: 456 },
        { id: '3', title: 'Value 3', description: 'Description 3', issueUrl: 'http://test3.com', issueNumber: 789 },
      ];
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.businessValues()).toEqual(mockBusinessValues);
      expect(component.businessValues().length).toBe(3);
    });

    it('should handle empty business values array', () => {
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of([]));

      component.releaseId = 'test-release-id';
      component.ngOnChanges({
        releaseId: {
          currentValue: 'test-release-id',
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.businessValues()).toEqual([]);
    });
  });
});
