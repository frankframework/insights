import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { BusinessValueEditComponent } from './business-value-edit.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

function createInputEvent(value: string): Event {
  return { target: { value } } as any;
}

describe('BusinessValueEditComponent', () => {
  let component: BusinessValueEditComponent;
  let fixture: ComponentFixture<BusinessValueEditComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;

  const mockBusinessValue: BusinessValue = {
    id: 'bv-123',
    title: 'Original Title',
    description: 'Original Description',
    issues: [],
  };

  const updatedBusinessValue: BusinessValue = {
    id: 'bv-123',
    title: 'Updated Title',
    description: 'Updated Description',
    issues: [],
  };

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['updateBusinessValue']);

    await TestBed.configureTestingModule({
      imports: [BusinessValueEditComponent],
      providers: [
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessValueEditComponent);
    component = fixture.componentInstance;
    component.businessValue = mockBusinessValue;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should populate form fields with businessValue data on input change (setter)', () => {
      expect(component.name()).toBe('Original Title');
      expect(component.description()).toBe('Original Description');
    });

    it('should handle input change when businessValue is updated', () => {
      component.businessValue = {
        id: 'custom-id',
        title: 'Custom Title',
        description: 'Custom Description',
        issues: [],
      };
      fixture.detectChanges();

      expect(component.name()).toBe('Custom Title');
      expect(component.description()).toBe('Custom Description');
      expect(component.isFormValidAndChanged()).toBeFalse();
    });
  });

  describe('isFormValidAndChanged', () => {
    it('should be FALSE immediately after initialization (no changes)', () => {
      expect(component.name()).toBe('Original Title');
      expect(component.description()).toBe('Original Description');
      expect(component.isFormValidAndChanged()).toBeFalse();
    });

    it('should be TRUE when name is changed', () => {
      component.updateName(createInputEvent('New Title'));

      expect(component.isFormValidAndChanged()).toBeTrue();
    });

    it('should be TRUE when description is changed', () => {
      component.updateDescription(createInputEvent('New Description'));

      expect(component.isFormValidAndChanged()).toBeTrue();
    });

    it('should be FALSE when a field is changed back to original value', () => {
      component.updateName(createInputEvent('New Title'));

      expect(component.isFormValidAndChanged()).toBeTrue();

      component.updateName(createInputEvent('Original Title'));

      expect(component.isFormValidAndChanged()).toBeFalse();
    });

    it('should be FALSE if a field becomes empty', () => {
      component.updateName(createInputEvent('New Title'));

      expect(component.isFormValidAndChanged()).toBeTrue();

      component.updateName(createInputEvent(''));

      expect(component.isFormValidAndChanged()).toBeFalse();
    });
  });

  describe('Form Updates', () => {
    it('should update name when updateName is called', () => {
      component.updateName(createInputEvent('New Title'));

      expect(component.name()).toBe('New Title');
    });

    it('should update description when updateDescription is called', () => {
      component.updateDescription(createInputEvent('New Description'));

      expect(component.description()).toBe('New Description');
    });

    it('should clear error message when updating name if error exists', () => {
      component.errorMessage.set('Some error');

      component.updateName(createInputEvent('Valid Name'));

      expect(component.errorMessage()).toBe('');
    });

    it('should not affect error message when updating description', () => {
      component.errorMessage.set('Some error');

      component.updateDescription(createInputEvent('Valid Description'));

      expect(component.errorMessage()).toBe('Some error');
    });
  });

  describe('Validation', () => {
    it('should not call save when title is empty (isFormValidAndChanged is false)', () => {
      component.updateName(createInputEvent(''));
      component.updateDescription(createInputEvent('Valid Description'));

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description is empty', () => {
      component.updateName(createInputEvent('Valid Title'));
      component.updateDescription(createInputEvent(''));

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when title is only whitespace', () => {
      component.updateName(createInputEvent('   '));
      component.updateDescription(createInputEvent('Valid Description'));

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description is only whitespace', () => {
      component.updateName(createInputEvent('Valid Title'));
      component.updateDescription(createInputEvent('   '));

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when title exceeds 255 characters', () => {
      const longTitle = 'a'.repeat(256);
      component.updateName(createInputEvent(longTitle));
      component.updateDescription(createInputEvent('Valid Description'));

      component.save();

      expect(component.errorMessage()).toContain('Title cannot exceed 255 characters');
      expect(component.errorMessage()).toContain('256');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description exceeds 1000 characters', () => {
      const longDescription = 'a'.repeat(1001);
      component.updateName(createInputEvent('Valid Title'));
      component.updateDescription(createInputEvent(longDescription));

      component.save();

      expect(component.errorMessage()).toContain('Description cannot exceed 1000 characters');
      expect(component.errorMessage()).toContain('1001');
      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });

    it('should accept title with exactly 255 characters', () => {
      const maxTitle = 'a'.repeat(255);
      component.updateName(createInputEvent(maxTitle));
      component.updateDescription(createInputEvent('Valid Description'));
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(mockBusinessValueService.updateBusinessValue).toHaveBeenCalledWith(
        component.businessValue.id,
        maxTitle,
        'Valid Description',
      );
    });

    it('should accept description with exactly 1000 characters', () => {
      const maxDescription = 'a'.repeat(1000);
      component.updateName(createInputEvent('Valid Title'));
      component.updateDescription(createInputEvent(maxDescription));
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(mockBusinessValueService.updateBusinessValue).toHaveBeenCalledWith(
        component.businessValue.id,
        'Valid Title',
        maxDescription,
      );
    });
  });

  describe('Save Operation', () => {
    beforeEach(() => {
      component.updateName(createInputEvent('Updated Title'));
      component.updateDescription(createInputEvent('Updated Description'));
    });

    it('should call updateBusinessValue with correct parameters', () => {
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(mockBusinessValueService.updateBusinessValue).toHaveBeenCalledWith(
        'bv-123',
        'Updated Title',
        'Updated Description',
      );
    });

    it('should call updateBusinessValue with trimmed values', () => {
      component.updateName(createInputEvent('  Updated Title  '));
      component.updateDescription(createInputEvent('  Updated Description  '));
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(mockBusinessValueService.updateBusinessValue).toHaveBeenCalledWith(
        'bv-123',
        'Updated Title',
        'Updated Description',
      );
    });

    it('should set isSaving to true during save and false after completion', () => {
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(component.isSaving()).toBeFalse();
    });

    it('should emit businessValueUpdated on successful save', (done) => {
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.businessValueUpdated.subscribe((result) => {
        expect(result).toEqual(updatedBusinessValue);
        done();
      });

      component.save();
    });

    it('should emit closed event on successful save', (done) => {
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.closed.subscribe(() => {
        done();
      });

      component.save();
    });

    it('should clear error message before saving', () => {
      component.errorMessage.set('Previous error');
      mockBusinessValueService.updateBusinessValue.and.returnValue(of(updatedBusinessValue));

      component.save();

      expect(component.errorMessage()).toBe('');
    });

    it('should handle save error gracefully', () => {
      const errorResponse = { error: { message: 'Database error' } };
      mockBusinessValueService.updateBusinessValue.and.returnValue(throwError(() => errorResponse));

      component.save();

      expect(component.isSaving()).toBeFalse();
      expect(component.errorMessage()).toBe('Database error');
    });

    it('should show default error message when error has no message', () => {
      mockBusinessValueService.updateBusinessValue.and.returnValue(throwError(() => ({})));

      component.save();

      expect(component.isSaving()).toBeFalse();
      expect(component.errorMessage()).toBe('Failed to update business value');
    });

    it('should not emit events on error', () => {
      const errorResponse = { error: { message: 'Database error' } };
      mockBusinessValueService.updateBusinessValue.and.returnValue(throwError(() => errorResponse));

      let updatedEmitted = false;
      let closedEmitted = false;

      component.businessValueUpdated.subscribe(() => {
        updatedEmitted = true;
      });

      component.closed.subscribe(() => {
        closedEmitted = true;
      });

      component.save();

      expect(updatedEmitted).toBeFalse();
      expect(closedEmitted).toBeFalse();
    });

    it('should not call updateBusinessValue when there are no changes', () => {
      component.businessValue = mockBusinessValue;
      fixture.detectChanges();

      expect(component.isFormValidAndChanged()).toBeFalse();

      component.save();

      expect(mockBusinessValueService.updateBusinessValue).not.toHaveBeenCalled();
    });
  });

  describe('Close Operation', () => {
    it('should emit closed event when close is called', (done) => {
      component.closed.subscribe(() => {
        done();
      });

      component.close();
    });
  });
});
