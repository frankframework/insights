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
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'main' },
};

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

  describe('Business Value Selection', () => {
    it('should select a business value and fetch its details', () => {
      const detailedBV = { ...mockBusinessValues[0], description: 'Detailed Desc' };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(detailedBV));

      component.selectBusinessValue(mockBusinessValues[0]);

      expect(component.selectedBusinessValue()?.id).toBe('bv-1');
      expect(mockBusinessValueService.getBusinessValueById).toHaveBeenCalledWith('bv-1');
    });

    it('should deselect business value when clicking the same one', () => {
      const detailedBV = { ...mockBusinessValues[0], description: 'Detailed Desc' };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(detailedBV));

      component.selectBusinessValue(mockBusinessValues[0]);

      expect(component.selectedBusinessValue()?.id).toBe('bv-1');

      component.selectBusinessValue(mockBusinessValues[0]);

      expect(component.selectedBusinessValue()).toBeNull();
    });

    it('should update issue selection when business value is selected', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));

      component.selectBusinessValue(mockBusinessValues[0]);

      const issues = component.issuesWithSelection();
      const issue1 = issues.find(index => index.id === 'issue-1');

      expect(issue1?.isConnected).toBeTrue();
      expect(issue1?.isSelected).toBeTrue();
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
      const newBV: BusinessValue = { id: 'new-bv', title: 'New', description: 'New Desc', issues: [] };

      mockBusinessValueService.getBusinessValueById.and.returnValue(of(newBV));

      component.onBusinessValueCreated(newBV);

      expect(component.businessValues().length).toBe(3);
      expect(component.selectedBusinessValue()?.id).toBe('new-bv');
      expect(mockBusinessValueService.getBusinessValueById).toHaveBeenCalledWith('new-bv');
    });
  });

  describe('Cross-release issue preservation', () => {
    it('should preserve issues from other releases when saving', () => {
      const otherReleaseIssue = { id: 'issue-4', number: 104, title: 'Other release issue', state: 'OPEN', url: '' };
      const bvWithOtherReleaseIssue: BusinessValue = {
        ...mockBusinessValues[0],
        issues: [mockIssues[0], otherReleaseIssue as any],
      };

      mockBusinessValueService.getBusinessValueById.and.returnValue(of(bvWithOtherReleaseIssue));
      component.selectBusinessValue(mockBusinessValues[0]);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(bvWithOtherReleaseIssue));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;

      expect(sentIds).toContain('issue-4');
      expect(sentIds).toContain('issue-1');
    });

    it('should not duplicate issues that exist in both current release and already connected', () => {
      const bv: BusinessValue = { ...mockBusinessValues[0], issues: [mockIssues[0]] };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(bv));
      component.selectBusinessValue(mockBusinessValues[0]);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(bv));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;
      const issue1Count = sentIds.filter((id: string) => id === 'issue-1').length;

      expect(issue1Count).toBe(1);
    });

    it('should allow deselecting an issue from the current release without affecting other-release issues', () => {
      const otherReleaseIssue = { id: 'issue-4', number: 104, title: 'Other release issue', state: 'OPEN', url: '' };
      const bvWithBoth: BusinessValue = {
        ...mockBusinessValues[0],
        issues: [mockIssues[0], mockIssues[1], otherReleaseIssue as any],
      };

      mockBusinessValueService.getBusinessValueById.and.returnValue(of(bvWithBoth));
      component.selectBusinessValue(mockBusinessValues[0]);

      const issue1 = component.issuesWithSelection().find(issue => issue.id === 'issue-1')!;
      component.toggleIssue(issue1);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(bvWithBoth));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;

      expect(sentIds).not.toContain('issue-1');
      expect(sentIds).toContain('issue-2');
      expect(sentIds).toContain('issue-4');
    });

    it('should send only other-release issues when no current-release issues are selected', () => {
      const otherReleaseIssue = { id: 'issue-4', number: 104, title: 'Other release issue', state: 'OPEN', url: '' };
      const bvWithOnlyOtherRelease: BusinessValue = {
        ...mockBusinessValues[1],
        issues: [otherReleaseIssue as any],
      };

      mockBusinessValueService.getBusinessValueById.and.returnValue(of(bvWithOnlyOtherRelease));
      component.selectBusinessValue(mockBusinessValues[1]);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(bvWithOnlyOtherRelease));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;

      expect(sentIds).toContain('issue-4');
      expect(sentIds.length).toBe(1);
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
