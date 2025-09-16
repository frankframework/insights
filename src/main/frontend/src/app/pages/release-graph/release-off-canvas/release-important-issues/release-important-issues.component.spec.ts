import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { ReleaseImportantIssuesComponent } from './release-important-issues.component';
import { Issue } from '../../../../services/issue.service';
import { ReleaseOffCanvasComponent } from '../release-off-canvas.component';
import { GitHubStates } from '../../../../app.service';

const createMockIssue = (id: string, number: number, typeName: string | null, subIssues: Issue[] = []): Issue => ({
  id,
  number,
  title: `Issue ${number}`,
  state: GitHubStates.OPEN,
  url: '',
  issueType: typeName ? { id: typeName, name: typeName, description: '', color: '' } : undefined,
  subIssues,
});

const mockOffCanvasComponent = {
  colorNameToRgba: (color: string) => `rgba(${color},0.75)`,
};

describe('ReleaseImportantIssuesComponent', () => {
  let component: ReleaseImportantIssuesComponent;
  let fixture: ComponentFixture<ReleaseImportantIssuesComponent>;

  const epic = createMockIssue('epic-1', 1, 'Epic');
  const featureWithSubs = createMockIssue('feat-1', 2, 'Feature', [createMockIssue('sub-1', 3, 'Task')]);
  const bug1 = createMockIssue('bug-1', 10, 'Bug');
  const bug2 = createMockIssue('bug-2', 5, 'Bug');
  const task = createMockIssue('task-1', 4, 'Task');
  const noTypeIssue = createMockIssue('no-type-1', 100, null);
  const mockIssues: Issue[] = [task, featureWithSubs, bug1, epic, noTypeIssue, bug2];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseImportantIssuesComponent],
      providers: [{ provide: ReleaseOffCanvasComponent, useValue: mockOffCanvasComponent }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseImportantIssuesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges', () => {
    it('should update the issuesSignal and reset the filter when releaseIssues input changes', () => {
      component.selectedType.set('Bug'); // Set a non-default filter
      component.releaseIssues = mockIssues;
      component.ngOnChanges({ releaseIssues: new SimpleChange(null, mockIssues, true) });

      expect((component as any).issuesSignal()).toEqual(mockIssues);
      expect(component.selectedType()).toBe('all');
    });

    it('should handle an empty or undefined releaseIssues input gracefully', () => {
      component.releaseIssues = undefined;
      component.ngOnChanges({ releaseIssues: new SimpleChange(null, undefined, true) });

      expect((component as any).issuesSignal()).toEqual([]);

      component.releaseIssues = [];
      component.ngOnChanges({ releaseIssues: new SimpleChange(undefined, [], true) });

      expect((component as any).issuesSignal()).toEqual([]);
    });
  });

  describe('Computed Signals', () => {
    beforeEach(() => {
      component.releaseIssues = mockIssues;
      component.ngOnChanges({ releaseIssues: new SimpleChange(null, mockIssues, true) });
      fixture.detectChanges();
    });

    it('issueTypeOptions should generate a sorted and unique list of filter options', () => {
      const options = component.issueTypeOptions();

      expect(options.length).toBe(6);
      expect(options.map((o) => o.label)).toEqual(['All types', 'Epic', 'Feature', 'Bug', 'Task', '(No type)']);
      expect(options[0].value).toBe('all');
      expect(options.find((o) => o.label === '(No type)')?.value).toBeNull();
    });

    it('sortedAndFilteredIssues should return all issues sorted correctly when filter is "all"', () => {
      const sorted = component.sortedAndFilteredIssues();

      expect(sorted.map((index) => index.id)).toEqual(['epic-1', 'feat-1', 'bug-2', 'bug-1', 'task-1', 'no-type-1']);
    });

    it('sortedAndFilteredIssues should filter by a specific issue type (Bug)', () => {
      component.selectedType.set('Bug');
      fixture.detectChanges();

      const filtered = component.sortedAndFilteredIssues();

      expect(filtered.length).toBe(2);
      expect(filtered.every((index) => index.issueType?.name === 'Bug')).toBe(true);
      expect(filtered.map((index) => index.id)).toEqual(['bug-2', 'bug-1']); // Check sorting within the filter
    });

    it('sortedAndFilteredIssues should filter for issues with no type when filter is null', () => {
      component.selectedType.set(null);
      fixture.detectChanges();

      const filtered = component.sortedAndFilteredIssues();

      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('no-type-1');
    });
  });
});
