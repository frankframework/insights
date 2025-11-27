import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseBusinessValueModalComponent } from './release-business-value-modal.component';
import { BusinessValueService } from '../../../services/business-value.service';
import { of, throwError } from 'rxjs';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ModalComponent } from '../../../components/modal/modal.component';
import { IssueTreeBranchComponent } from '../release-important-issues/issue-tree-branch/issue-tree-branch.component';
import { Issue } from '../../../services/issue.service';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: '<ng-content></ng-content>'
})
class MockModalComponent {
  @Input() title: string = '';
  @Output() closed = new EventEmitter<void>();
}

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  template: ''
})
class MockIssueTreeBranchComponent {
  @Input() issue!: Issue;
  @Input() depth: number = 0;
}

describe('ReleaseBusinessValueModalComponent', () => {
  let component: ReleaseBusinessValueModalComponent;
  let fixture: ComponentFixture<ReleaseBusinessValueModalComponent>;
  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;

  const mockIssue: Issue = {
    id: 'issue-1',
    number: 1,
    title: 'Test Issue',
    state: 'OPEN',
    url: 'http://github.com/test/1',
    labels: [],
  };

  const mockBusinessValue = {
    id: 'bv-1',
    title: 'Test BV',
    description: 'Test Description',
    issues: [mockIssue]
  };

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', ['getBusinessValueById']);

    await TestBed.configureTestingModule({
      imports: [ReleaseBusinessValueModalComponent],
      providers: [{ provide: BusinessValueService, useValue: mockBusinessValueService }],
    })
            .overrideComponent(ReleaseBusinessValueModalComponent, {
              remove: { imports: [ModalComponent, IssueTreeBranchComponent] },
              add: { imports: [MockModalComponent, MockIssueTreeBranchComponent] }
            })
            .compileComponents();

    fixture = TestBed.createComponent(ReleaseBusinessValueModalComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load business value details if businessValueId is present', () => {
      component.businessValueId = 'bv-1';
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValue));

      component.ngOnInit();

      expect(mockBusinessValueService.getBusinessValueById).toHaveBeenCalledWith('bv-1');
      expect(component.businessValue()).toEqual(mockBusinessValue);
      expect(component.isLoading()).toBeFalse();
    });

    it('should not load if businessValueId is missing', () => {
      component.ngOnInit();

      expect(mockBusinessValueService.getBusinessValueById).not.toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should handle errors when loading details', () => {
      component.businessValueId = 'bv-1';
      const errorResponse = { error: { message: 'Not Found' } };
      mockBusinessValueService.getBusinessValueById.and.returnValue(throwError(() => errorResponse));

      component.ngOnInit();

      expect(component.isLoading()).toBeFalse();
      expect(component.errorMessage()).toBe('Not Found');
    });

    it('should use default error message if error object has no message', () => {
      component.businessValueId = 'bv-1';
      mockBusinessValueService.getBusinessValueById.and.returnValue(throwError(() => new Error('Unknown')));

      component.ngOnInit();

      expect(component.errorMessage()).toBe('Failed to load business value details');
    });
  });

  describe('User Interactions', () => {
    it('should emit closed event when close() is called', () => {
      spyOn(component.closed, 'emit');
      component.close();

      expect(component.closed.emit).toHaveBeenCalledWith();
    });

    it('should open issue url in new tab', () => {
      spyOn(globalThis, 'open');
      component.openIssue(mockIssue);

      expect(globalThis.open).toHaveBeenCalledWith(mockIssue.url, '_blank');
    });

    it('should not open window if issue has no url', () => {
      spyOn(globalThis, 'open');
      const noUrlIssue = { ...mockIssue, url: undefined };
      component.openIssue(noUrlIssue as any);

      expect(globalThis.open).not.toHaveBeenCalled();
    });
  });
});
