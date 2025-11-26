import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { ReleaseBusinessValueComponent } from './release-business-value.component';
import { BusinessValueService } from '../../../services/business-value.service';
import { AuthService, User } from '../../../services/auth.service';
import { of, throwError } from 'rxjs';

describe('ReleaseBusinessValueComponent', () => {
  let component: ReleaseBusinessValueComponent;
  let fixture: ComponentFixture<ReleaseBusinessValueComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let consoleErrorSpy: jasmine.Spy;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://example.com/avatar.jpg',
    isFrankFrameworkMember: true,
  };

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['getBusinessValuesByReleaseId']);
    mockAuthService = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal<User | null>(null),
    });
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    consoleErrorSpy = spyOn(globalThis.console, 'error');

    await TestBed.configureTestingModule({
      imports: [ReleaseBusinessValueComponent],
      providers: [
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
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

  describe('FrankFramework member button', () => {
    it('should show the manage button when user is a FrankFramework member', () => {
      mockAuthService.currentUser = signal<User | null>(mockUser);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('.ff-member-button');

      expect(button).toBeTruthy();
      expect(button?.textContent?.trim()).toBe('Manage Business Values');
    });

    it('should not show the manage button when user is not a FrankFramework member', () => {
      const nonMemberUser: User = { ...mockUser, isFrankFrameworkMember: false };
      mockAuthService.currentUser = signal<User | null>(nonMemberUser);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('.ff-member-button');

      expect(button).toBeNull();
    });

    it('should not show the manage button when user is not logged in', () => {
      mockAuthService.currentUser = signal<User | null>(null);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector('.ff-member-button');

      expect(button).toBeNull();
    });

    it('should navigate to ff-members page when button is clicked', () => {
      mockAuthService.currentUser = signal<User | null>(mockUser);
      fixture.detectChanges();

      component.navigateToMembersArea();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/ff-members']);
    });
  });
});
