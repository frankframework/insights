import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BusinessValueDeleteComponent } from './business-value-delete.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

describe('BusinessValueDeleteComponent', () => {
  let component: BusinessValueDeleteComponent;
  let fixture: ComponentFixture<BusinessValueDeleteComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;

  const mockBusinessValue: BusinessValue = {
    id: 'bv-123',
    title: 'Business Value to Delete',
    description: 'This will be deleted',
    issues: [],
  };

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['deleteBusinessValue']);

    await TestBed.configureTestingModule({
      imports: [BusinessValueDeleteComponent],
      providers: [
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessValueDeleteComponent);
    component = fixture.componentInstance;
    component.businessValue = mockBusinessValue;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with correct default values', () => {
      expect(component.isDeleting()).toBeFalse();
      expect(component.errorMessage()).toBe('');
    });

    it('should have businessValue input set', () => {
      expect(component.businessValue).toEqual(mockBusinessValue);
      expect(component.businessValue.id).toBe('bv-123');
    });
  });

  describe('Delete Operation', () => {
    it('should call deleteBusinessValue with correct ID', () => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.confirmDelete();

      expect(mockBusinessValueService.deleteBusinessValue).toHaveBeenCalledWith('bv-123');
    });

    it('should set isDeleting to true during delete and false after completion', () => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.confirmDelete();

      expect(component.isDeleting()).toBeFalse();
    });

    it('should clear error message before deletion', () => {
      component.errorMessage.set('Previous error');
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.confirmDelete();

      expect(component.errorMessage()).toBe('');
    });

    it('should emit businessValueDeleted with ID on successful deletion', (done) => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.businessValueDeleted.subscribe((deletedId) => {
        expect(deletedId).toBe('bv-123');
        done();
      });

      component.confirmDelete();
    });

    it('should emit closed event on successful deletion', (done) => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.closed.subscribe(() => {
        done();
      });

      component.confirmDelete();
    });

    it('should handle deletion error gracefully', () => {
      const errorResponse = { error: { message: 'Cannot delete: has dependencies' } };
      mockBusinessValueService.deleteBusinessValue.and.returnValue(throwError(() => errorResponse));

      component.confirmDelete();

      expect(component.isDeleting()).toBeFalse();
      expect(component.errorMessage()).toBe('Cannot delete: has dependencies');
    });

    it('should show default error message when error has no message', () => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(throwError(() => ({})));

      component.confirmDelete();

      expect(component.isDeleting()).toBeFalse();
      expect(component.errorMessage()).toBe('Failed to delete business value');
    });

    it('should not emit events on deletion error', () => {
      const errorResponse = { error: { message: 'Database error' } };
      mockBusinessValueService.deleteBusinessValue.and.returnValue(throwError(() => errorResponse));

      let deletedEmitted = false;
      let closedEmitted = false;

      component.businessValueDeleted.subscribe(() => {
        deletedEmitted = true;
      });

      component.closed.subscribe(() => {
        closedEmitted = true;
      });

      component.confirmDelete();

      expect(deletedEmitted).toBeFalse();
      expect(closedEmitted).toBeFalse();
    });

    it('should handle network timeout error', () => {
      const timeoutError = { error: { message: 'Request timeout' } };
      mockBusinessValueService.deleteBusinessValue.and.returnValue(throwError(() => timeoutError));

      component.confirmDelete();

      expect(component.isDeleting()).toBeFalse();
      expect(component.errorMessage()).toBe('Request timeout');
    });
  });

  describe('Close Operation', () => {
    it('should emit closed event when close is called', (done) => {
      component.closed.subscribe(() => {
        done();
      });

      component.close();
    });

    it('should not call deleteBusinessValue when close is called', () => {
      component.close();

      expect(mockBusinessValueService.deleteBusinessValue).not.toHaveBeenCalled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle multiple consecutive delete attempts', () => {
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.confirmDelete();
      component.confirmDelete();

      expect(mockBusinessValueService.deleteBusinessValue).toHaveBeenCalledTimes(2);
    });

    it('should maintain businessValue reference throughout component lifecycle', () => {
      const initialBV = component.businessValue;

      expect(component.businessValue).toBe(initialBV);
      expect(component.businessValue.id).toBe('bv-123');
    });

    it('should handle deletion with business value that has associated issues', () => {
      component.businessValue = {
        id: 'bv-456',
        title: 'BV with Issues',
        description: 'Has issues',
        issues: [
          { id: 'issue-1', number: 101, title: 'Issue 1', state: 'OPEN', url: '' },
          { id: 'issue-2', number: 102, title: 'Issue 2', state: 'CLOSED', url: '' },
        ],
      };
      mockBusinessValueService.deleteBusinessValue.and.returnValue(of(void 0));

      component.confirmDelete();

      expect(mockBusinessValueService.deleteBusinessValue).toHaveBeenCalledWith('bv-456');
    });
  });

  describe('Error Message Display', () => {
    it('should display different error types correctly', () => {
      const errors = [
        { error: { error: { message: 'Not found' } }, expected: 'Not found' },
        { error: { error: { message: 'Unauthorized' } }, expected: 'Unauthorized' },
        { error: { error: { message: 'Server error' } }, expected: 'Server error' },
        { error: { error: {} }, expected: 'Failed to delete business value' },
      ];

      for (const testCase of errors) {
        component.errorMessage.set('');
        mockBusinessValueService.deleteBusinessValue.and.returnValue(throwError(() => testCase.error));

        component.confirmDelete();

        expect(component.errorMessage()).toBe(testCase.expected);
      }
    });
  });
});
