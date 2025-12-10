import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BusinessValueIssuePanelComponent, IssueWithSelection } from './business-value-issue-panel.component';
import { BusinessValue } from '../../../../services/business-value.service';

const mockBusinessValue: BusinessValue = {
  id: 'bv-1',
  title: 'Security Improvements',
  description: 'desc',
  issues: [{ id: 'issue-1' } as any],
};

const mockIssuesWithSelection: IssueWithSelection[] = [
  {
    id: 'issue-1',
    number: 101,
    title: 'Fix login bug',
    state: 'OPEN',
    url: '',
    isSelected: true,
    isConnected: true,
  },
  {
    id: 'issue-2',
    number: 102,
    title: 'Add dark mode',
    state: 'OPEN',
    url: '',
    isSelected: false,
    isConnected: false,
  },
  {
    id: 'issue-3',
    number: 103,
    title: 'Update documentation',
    state: 'OPEN',
    url: '',
    isSelected: false,
    isConnected: false,
    assignedToOther: true,
    assignedBusinessValueTitle: 'UX Enhancements',
  },
];

function createInputEvent(value: string): Event {
  return { target: { value } } as any;
}

describe('BusinessValueIssuePanelComponent', () => {
  let component: BusinessValueIssuePanelComponent;
  let fixture: ComponentFixture<BusinessValueIssuePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusinessValueIssuePanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessValueIssuePanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('selectedBusinessValue', mockBusinessValue);
    fixture.componentRef.setInput('issuesWithSelection', mockIssuesWithSelection);
    fixture.componentRef.setInput('hasChanges', false);
    fixture.componentRef.setInput('isSaving', false);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Filtering and Sorting', () => {
    it('should filter issues by title', () => {
      component.updateSearchQuery(createInputEvent('login'));
      const sorted = component.sortedIssues();

      expect(sorted.length).toBe(1);
      expect(sorted[0].id).toBe('issue-1');
    });

    it('should filter issues by number', () => {
      component.updateSearchQuery(createInputEvent('102'));
      const sorted = component.sortedIssues();

      expect(sorted.length).toBe(1);
      expect(sorted[0].id).toBe('issue-2');
    });

    it('should return all issues when search query is empty', () => {
      component.updateSearchQuery(createInputEvent(''));
      const sorted = component.sortedIssues();

      expect(sorted.length).toBe(3);
    });

    it('should be case insensitive when filtering', () => {
      component.updateSearchQuery(createInputEvent('DARK MODE'));
      const sorted = component.sortedIssues();

      expect(sorted.length).toBe(1);
      expect(sorted[0].id).toBe('issue-2');
    });

    it('should sort connected issues first', () => {
      const sorted = component.sortedIssues();

      expect(sorted[0].isConnected).toBeTrue();
      expect(sorted[0].id).toBe('issue-1');
    });

    it('should sort free issues before assigned issues', () => {
      const sorted = component.sortedIssues();

      expect(sorted[1].assignedToOther).toBeFalsy();
      expect(sorted[2].assignedToOther).toBeTrue();
    });

    it('should sort by issue number within the same priority group', () => {
      const sorted = component.sortedIssues();

      expect(sorted[1].number).toBeLessThan(sorted[2].number);
    });
  });

  describe('Event Emissions', () => {
    it('should emit issueToggled when toggling an issue', () => {
      spyOn(component.issueToggled, 'emit');

      component.toggleIssue(mockIssuesWithSelection[0]);

      expect(component.issueToggled.emit).toHaveBeenCalledWith(mockIssuesWithSelection[0]);
    });

    it('should emit saveClicked when save button is clicked', () => {
      spyOn(component.saveClicked, 'emit');

      component.onSaveClick();

      expect(component.saveClicked.emit).toHaveBeenCalledWith();
    });
  });

  describe('Search Query Management', () => {
    it('should update search query signal when input changes', () => {
      const query = 'test query';
      component.updateSearchQuery(createInputEvent(query));

      expect(component.issueSearchQuery()).toBe(query);
    });

    it('should trim and lowercase search query for filtering', () => {
      component.updateSearchQuery(createInputEvent('  LOGIN  '));
      const sorted = component.sortedIssues();

      expect(sorted.length).toBe(1);
      expect(sorted[0].title).toBe('Fix login bug');
    });
  });

  describe('No Selection State', () => {
    it('should handle null selectedBusinessValue', () => {
      fixture.componentRef.setInput('selectedBusinessValue', null);
      fixture.detectChanges();

      expect(component.selectedBusinessValue()).toBeNull();
    });
  });
});
