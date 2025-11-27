import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BusinessValueAddComponent } from './business-value-add.component';
import { BusinessValueService, BusinessValue } from '../../../../services/business-value.service';

describe('BusinessValueAddComponent', () => {
  let component: BusinessValueAddComponent;
  let fixture: ComponentFixture<BusinessValueAddComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;

  const mockBusinessValue: BusinessValue = {
    id: 'bv-123',
    title: 'Test Business Value',
    description: 'Test Description',
    issues: [],
  };

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['createBusinessValue']);

    await TestBed.configureTestingModule({
      imports: [BusinessValueAddComponent],
      providers: [
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessValueAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with empty form fields', () => {
      expect(component.name()).toBe('');
      expect(component.description()).toBe('');
      expect(component.isSaving()).toBeFalse();
      expect(component.errorMessage()).toBe('');
    });
  });

  describe('Form Updates', () => {
    it('should update name when updateName is called', () => {
      component.updateName('New Title');

      expect(component.name()).toBe('New Title');
    });

    it('should update description when updateDescription is called', () => {
      component.updateDescription('New Description');

      expect(component.description()).toBe('New Description');
    });

    it('should clear error message when updating name if error exists', () => {
      component.errorMessage.set('Some error');

      component.updateName('Valid Name');

      expect(component.errorMessage()).toBe('');
    });

    it('should not affect error message when updating description', () => {
      component.errorMessage.set('Some error');

      component.updateDescription('Valid Description');

      expect(component.errorMessage()).toBe('Some error');
    });
  });

  describe('Validation', () => {
    it('should show error when title is empty', () => {
      component.updateName('');
      component.updateDescription('Valid Description');

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description is empty', () => {
      component.updateName('Valid Title');
      component.updateDescription('');

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when title is only whitespace', () => {
      component.updateName('   ');
      component.updateDescription('Valid Description');

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description is only whitespace', () => {
      component.updateName('Valid Title');
      component.updateDescription('   ');

      component.save();

      expect(component.errorMessage()).toBe('Both title and description are required');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when title exceeds 255 characters', () => {
      const longTitle = 'a'.repeat(256);
      component.updateName(longTitle);
      component.updateDescription('Valid Description');

      component.save();

      expect(component.errorMessage()).toContain('Title cannot exceed 255 characters');
      expect(component.errorMessage()).toContain('256');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should show error when description exceeds 1000 characters', () => {
      const longDescription = 'a'.repeat(1001);
      component.updateName('Valid Title');
      component.updateDescription(longDescription);

      component.save();

      expect(component.errorMessage()).toContain('Description cannot exceed 1000 characters');
      expect(component.errorMessage()).toContain('1001');
      expect(mockBusinessValueService.createBusinessValue).not.toHaveBeenCalled();
    });

    it('should accept title with exactly 255 characters', () => {
      const maxTitle = 'a'.repeat(255);
      component.updateName(maxTitle);
      component.updateDescription('Valid Description');
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.save();

      expect(mockBusinessValueService.createBusinessValue).toHaveBeenCalledWith(maxTitle, 'Valid Description');
    });

    it('should accept description with exactly 1000 characters', () => {
      const maxDescription = 'a'.repeat(1000);
      component.updateName('Valid Title');
      component.updateDescription(maxDescription);
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.save();

      expect(mockBusinessValueService.createBusinessValue).toHaveBeenCalledWith('Valid Title', maxDescription);
    });
  });

  describe('Save Operation', () => {
    beforeEach(() => {
      component.updateName('Test Title');
      component.updateDescription('Test Description');
    });

    it('should call createBusinessValue with trimmed values on successful save', () => {
      component.updateName('  Test Title  ');
      component.updateDescription('  Test Description  ');
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.save();

      expect(mockBusinessValueService.createBusinessValue).toHaveBeenCalledWith('Test Title', 'Test Description');
    });

    it('should set isSaving to true during save', () => {
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.save();

      expect(component.isSaving()).toBeFalse();
    });

    it('should emit businessValueCreated on successful save', (done) => {
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.businessValueCreated.subscribe((result) => {
        expect(result).toEqual(mockBusinessValue);
        done();
      });

      component.save();
    });

    it('should emit closed event on successful save', (done) => {
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.closed.subscribe(() => {
        done();
      });

      component.save();
    });

    it('should clear error message before saving', () => {
      component.errorMessage.set('Previous error');
      mockBusinessValueService.createBusinessValue.and.returnValue(of(mockBusinessValue));

      component.save();

      expect(component.errorMessage()).toBe('');
    });

    it('should handle save error gracefully', () => {
      const errorResponse = { error: { message: 'Database error' } };
      mockBusinessValueService.createBusinessValue.and.returnValue(throwError(() => errorResponse));

      component.save();

      expect(component.isSaving()).toBeFalse();
      expect(component.errorMessage()).toBe('Database error');
    });

    it('should show default error message when error has no message', () => {
      mockBusinessValueService.createBusinessValue.and.returnValue(throwError(() => ({})));

      component.save();

      expect(component.isSaving()).toBeFalse();
      expect(component.errorMessage()).toBe('Failed to create business value');
    });

    it('should not emit events on error', () => {
      const errorResponse = { error: { message: 'Database error' } };
      mockBusinessValueService.createBusinessValue.and.returnValue(throwError(() => errorResponse));

      let createdEmitted = false;
      let closedEmitted = false;

      component.businessValueCreated.subscribe(() => {
        createdEmitted = true;
      });

      component.closed.subscribe(() => {
        closedEmitted = true;
      });

      component.save();

      expect(createdEmitted).toBeFalse();
      expect(closedEmitted).toBeFalse();
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
