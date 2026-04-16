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
  @Input() releaseId!: string;
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
  { id: 'bv-1', title: 'Security Improvements', description: 'desc', releaseId: 'release-123', issues: [mockIssues[0]] },
  { id: 'bv-2', title: 'UX Enhancements', description: 'desc', releaseId: 'release-123', issues: [] },
];

const mockRelease: Release = {
  id: 'release-123',
  name: 'v2.0',
  tagName: 'v2.0',
  publishedAt: new Date(),
  lastScanned: new Date(),
  branch: { id: 'b1', name: 'main' },
};

const mockOtherRelease: Release = {
  id: 'release-456',
  name: 'v1.0',
  tagName: 'v1.0',
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
      'getBusinessValuesByReleaseId',
      'getBusinessValueById',
      'updateIssueConnections',
      'duplicateBusinessValues',
    ]);
    mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getReleaseById', 'getAllReleases']);
    mockLocation = jasmine.createSpyObj('Location', ['back']);

    mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(of(mockBusinessValues));
    mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));
    mockReleaseService.getReleaseById.and.returnValue(of(mockRelease));
    mockReleaseService.getAllReleases.and.returnValue(of([mockRelease, mockOtherRelease]));

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
          imports: [MockBusinessValueAddComponent, MockBusinessValueEditComponent, MockBusinessValueDeleteComponent],
        },
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
    it('should fetch business values by releaseId, issues, release info, and all releases on init', () => {
      expect(mockBusinessValueService.getBusinessValuesByReleaseId).toHaveBeenCalledWith('release-123');
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-123');
      expect(mockReleaseService.getReleaseById).toHaveBeenCalledWith('release-123');
      expect(mockReleaseService.getAllReleases).toHaveBeenCalledWith();

      expect(component.isLoading()).toBeFalse();
      expect(component.businessValues().length).toBe(2);
      expect(component.allIssues().length).toBe(3);
      expect(component.releaseTitle()).toBe('v2.0');
    });

    it('should populate otherReleases excluding the current release', () => {
      const others = component.otherReleases();

      expect(others.length).toBe(1);
      expect(others[0].id).toBe('release-456');
    });

    it('should handle error during initialization gracefully', () => {
      mockBusinessValueService.getBusinessValuesByReleaseId.and.returnValue(throwError(() => new Error('API Error')));

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
      const issue1 = issues.find((index) => index.id === 'issue-1');

      expect(issue1?.isConnected).toBeTrue();
      expect(issue1?.isSelected).toBeTrue();
    });
  });

  describe('Save Changes', () => {
    it('should call updateIssueConnections with only current-release selected issues', () => {
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(mockBusinessValues[0]));
      component.selectBusinessValue(mockBusinessValues[0]);

      const issue2 = component.issuesWithSelection().find((index) => index.id === 'issue-2')!;
      component.toggleIssue(issue2);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(mockBusinessValues[0]));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;

      expect(sentIds).toContain('issue-1');
      expect(sentIds).toContain('issue-2');
      expect(component.isSaving()).toBeFalse();
    });

    it('should not include issues from other releases (BVs are now release-scoped)', () => {
      const bvWithOnlyCurrentIssues: BusinessValue = {
        ...mockBusinessValues[0],
        issues: [mockIssues[0]],
      };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(bvWithOnlyCurrentIssues));
      component.selectBusinessValue(mockBusinessValues[0]);

      mockBusinessValueService.updateIssueConnections.and.returnValue(of(bvWithOnlyCurrentIssues));
      component.saveChanges();

      const [, sentIds] = mockBusinessValueService.updateIssueConnections.calls.mostRecent().args;

      expect(sentIds).toEqual(['issue-1']);
    });
  });

  describe('CRUD Actions', () => {
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

      expect(list.find((b) => b.id === 'bv-1')?.title).toBe('New Title');
    });

    it('should handle create business value', () => {
      const newBV: BusinessValue = { id: 'new-bv', title: 'New', description: 'New Desc', releaseId: 'release-123', issues: [] };
      mockBusinessValueService.getBusinessValueById.and.returnValue(of(newBV));

      component.onBusinessValueCreated(newBV);

      expect(component.businessValues().length).toBe(3);
      expect(component.selectedBusinessValue()?.id).toBe('new-bv');
    });
  });

  describe('Duplicate from release', () => {
    it('should open and close the duplicate modal', () => {
      expect(component.showDuplicateModal()).toBeFalse();

      component.openDuplicateModal();

      expect(component.showDuplicateModal()).toBeTrue();

      component.closeDuplicateModal();

      expect(component.showDuplicateModal()).toBeFalse();
    });

    it('should call duplicateBusinessValues and add results to the list', () => {
      const duplicated: BusinessValue[] = [
        { id: 'bv-dup-1', title: 'Security Improvements', description: 'desc', releaseId: 'release-123', issues: [] },
      ];
      mockBusinessValueService.duplicateBusinessValues.and.returnValue(of(duplicated));

      component.openDuplicateModal();
      component.duplicateFromRelease(mockOtherRelease);

      expect(mockBusinessValueService.duplicateBusinessValues).toHaveBeenCalledWith('release-123', 'release-456');
      expect(component.businessValues().length).toBe(3);
      expect(component.showDuplicateModal()).toBeFalse();
    });

    it('should show error message when duplicate fails', () => {
      mockBusinessValueService.duplicateBusinessValues.and.returnValue(
        throwError(() => ({ error: { message: 'Duplicate failed' } })),
      );

      component.openDuplicateModal();
      component.duplicateFromRelease(mockOtherRelease);

      expect(component.duplicateErrorMessage()).toBe('Duplicate failed');
      expect(component.showDuplicateModal()).toBeTrue();
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
