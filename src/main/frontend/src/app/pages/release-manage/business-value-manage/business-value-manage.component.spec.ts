import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { BusinessValueManageComponent } from './business-value-manage.component';
import { BusinessValueService, BusinessValue } from '../../../services/business-value.service';
import { IssueService, Issue } from '../../../services/issue.service';
import { ReleaseService, Release } from '../../../services/release.service';

@Component({ selector: 'app-business-value-add', standalone: true, template: '' })
class MockBusinessValueAddComponent {
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueCreated = new EventEmitter<BusinessValue>();
}

@Component({ selector: 'app-business-value-edit', standalone: true, template: '' })
class MockBusinessValueEditComponent {
  @Input() businessValue: any;
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueUpdated = new EventEmitter<BusinessValue>();
}

@Component({ selector: 'app-business-value-delete', standalone: true, template: '' })
class MockBusinessValueDeleteComponent {
  @Input() businessValue: any;
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueDeleted = new EventEmitter<string>();
}

const mockIssues: Issue[] = [
  { id: 'issue-1', number: 101, title: 'Fix login bug', state: 'OPEN', url: '' },
  { id: 'issue-2', number: 102, title: 'Add dark mode', state: 'OPEN', url: '' },
  { id: 'issue-3', number: 103, title: 'Update documentation', state: 'CLOSED', url: '' },
];

const mockBusinessValues: BusinessValue[] = [
  { id: 'bv-1', title: 'Security Improvements', description: 'desc', issues: [mockIssues[0]] },
  { id: 'bv-2', title: 'UX Enhancements', description: 'desc', issues: [] },
];

const mockRelease: Release = {
  id: 'release-123',
  name: 'v2.0',
  tagName: 'v2.0',
  publishedAt: new Date(),
  branch: { id: 'b1', name: 'main' },
};

function createInputEvent(value: string): Event {
  return { target: { value } } as any;
}

describe('BusinessValueManageComponent', () => {
  let component: BusinessValueManageComponent;
  let fixture: ComponentFixture<BusinessValueManageComponent>;

  let mockBusinessValueService: jasmine.SpyObj<BusinessValueService>;
  let mockIssueService: jasmine.SpyObj<IssueService>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockLocation: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    mockBusinessValueService = jasmine.createSpyObj('BusinessValueService', [
      'getAllBusinessValues',
      'getBusinessValueById',
      'updateIssueConnections'
    ]);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById']);
    mockLocation = jasmine.createSpyObj('Location', ['back']);
    mockBusinessValueService.getAllBusinessValues.and.returnValue(of(mockBusinessValues));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));

    await TestBed.configureTestingModule({
      imports: [BusinessValueManageComponent],
      providers: [
        { provide: BusinessValueService, useValue: mockBusinessValueService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: Location, useValue: mockLocation },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'release-123' } },
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })
            .overrideComponent(BusinessValueManageComponent, {
              add: {
                imports: [
                  MockBusinessValueAddComponent,
                  MockBusinessValueEditComponent,
                  MockBusinessValueDeleteComponent
                ]
              }
            })
            .compileComponents();

    fixture = TestBed.createComponent(BusinessValueManageComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization (ngOnInit)', () => {
    it('should fetch business values, issues, and release info on init', () => {
      expect(mockBusinessValueService.getAllBusinessValues).toHaveBeenCalledWith();
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-123');
      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-123');

      expect(component.isLoading()).toBeFalse();
      expect(component.businessValues().length).toBe(2);
      expect(component.allIssues().length).toBe(3);
      expect(component.releaseTitle()).toBe('v2.0');
    });

    it('should handle error during initialization gracefully', () => {
      mockBusinessValueService.getAllBusinessValues.and.returnValue(throwError(() => new Error('API Error')));

      component.ngOnInit();

      expect(component.isLoading()).toBeFalse();
      expect(component.businessValues().length).toBe(0);
    });
  });

  describe('Computed Properties & Filtering', () => {
    it('should filter business values by search query', () => {
      component.updateBusinessValueSearchQuery(createInputEvent('Security'));

      const filtered = component.filteredBusinessValues();

      expect(filtered.length).toBe(1);
      expect(filtered[0].title).toBe('Security Improvements');
    });

    it('should sort business values by issue count descending', () => {
      const sorted = component.filteredBusinessValues();

      expect(sorted[0].id).toBe('bv-1');
      expect(sorted[1].id).toBe('bv-2');
    });

    it('should filter sortedIssues by search query', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));
      component.selectBusinessValue(mockBusinessValues[0]);

      component.updateIssueSearchQuery(createInputEvent('login'));

      const issues = component.sortedIssues();

      expect(issues.length).toBe(1);
      expect(issues[0].id).toBe('issue-1');
    });
  });

  describe('Selection Logic (Complex)', () => {
    it('should select a business value and fetch its details', () => {
      const detailedBV = { ...mockBusinessValues[0], description: 'Detailed Desc' };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(detailedBV));

      component.selectBusinessValue(mockBusinessValues[0]);

      expect(component.selectedBusinessValue()?.id).toBe('bv-1');
      expect(mockBusinessValueService.getBusinessValueById).toHaveBeenCalledWith('bv-1');
    });

    it('should mark issues as connected if they belong to the selected BV', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));

      component.selectBusinessValue(mockBusinessValues[0]);

      const issues = component.issuesWithSelection();
      const issue1 = issues.find(index => index.id === 'issue-1');
      const issue2 = issues.find(index => index.id === 'issue-2');

      expect(issue1?.isConnected).toBeTrue();
      expect(issue1?.isSelected).toBeTrue();
      expect(issue2?.isConnected).toBeFalse();
    });

    it('should mark issues as assignedToOther if they belong to a different BV', () => {
      const bvWithIssue2 = { ...mockBusinessValues[1], issues: [mockIssues[1]] };
      component.businessValues.set([mockBusinessValues[0], bvWithIssue2]);

      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));
      component.selectBusinessValue(mockBusinessValues[0]);

      const issues = component.issuesWithSelection();
      const issue2 = issues.find(index => index.id === 'issue-2');

      expect(issue2?.assignedToOther).toBeTrue();
      expect(issue2?.assignedBusinessValueTitle).toBe('UX Enhancements');
    });

    it('should toggle issue selection correctly', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));
      component.selectBusinessValue(mockBusinessValues[0]);

      const issue1 = component.issuesWithSelection().find(index => index.id === 'issue-1')!;

      component.toggleIssue(issue1);

      expect(component.issuesWithSelection().find(index => index.id === 'issue-1')?.isSelected).toBeFalse();
      expect(component.hasChanges()).toBeTrue();

      component.toggleIssue(issue1);

      expect(component.issuesWithSelection().find(index => index.id === 'issue-1')?.isSelected).toBeTrue();
      expect(component.hasChanges()).toBeFalse();
    });

    it('should NOT toggle issue if it is assigned to another BV', () => {
      const issueAssigned = { ...mockIssues[0], assignedToOther: true, isSelected: false, isConnected: false };
      component.issuesWithSelection.set([issueAssigned]);

      component.toggleIssue(issueAssigned);

      expect(component.issuesWithSelection()[0].isSelected).toBeFalse();
    });
  });

  describe('CRUD Actions & Saving', () => {
    it('should call updateIssueConnections on save', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));
      component.selectBusinessValue(mockBusinessValues[0]);

      const issue2 = component.issuesWithSelection().find(index => index.id === 'issue-2')!;
      component.toggleIssue(issue2);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(mockBusinessValues[0]));

      component.saveChanges();

      expect(mockBusinessValueService.updateIssueConnections).toHaveBeenCalledWith(
              'bv-1',
              jasmine.arrayContaining(['issue-1', 'issue-2'])
      );

      expect(component.isSaving()).toBeFalse();
    });

    it('should handle delete business value', () => {
      // Open delete modal
      const event = new MouseEvent('click');
      component.openDeleteModal(mockBusinessValues[0], event);

      expect(component.showDeleteForm()).toBeTrue();
      expect(component.businessValueToDelete()?.id).toBe('bv-1');

      component.onBusinessValueDeleted('bv-1');

      expect(component.businessValues().length).toBe(1);
      expect(component.showDeleteForm()).toBeFalse();
    });

    it('should handle update business value', () => {
      const updated = { ...mockBusinessValues[0], title: 'New Title' };
      component.onBusinessValueUpdated(updated);

      const list = component.businessValues();

      expect(list.find(b => b.id === 'bv-1')?.title).toBe('New Title');
    });

    it('should handle create business value', () => {
      const newBV: BusinessValue = { id: 'new', title: 'New', description: '', issues: [] };
      component.onBusinessValueCreated(newBV);

      expect(component.businessValues().length).toBe(3);
    });
  });

  describe('UI Interaction', () => {
    it('should toggle create form visibility', () => {
      expect(component.showCreateForm()).toBeFalse();
      component.toggleCreateForm();

      expect(component.showCreateForm()).toBeTrue();
    });

    it('should go back using location service', () => {
      component.goBack();

      expect(mockLocation.back).toHaveBeenCalledWith();
    });
  });
});
