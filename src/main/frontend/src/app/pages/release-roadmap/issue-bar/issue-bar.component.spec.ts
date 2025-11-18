import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { IssueBarComponent } from './issue-bar.component';
import { Issue, IssuePriority, IssueState, IssueType } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { TooltipService } from './tooltip/tooltip.service';

const EPIC_TYPE: IssueType = {
  id: 'epic-1',
  name: 'Epic',
  description: 'Epic issue type',
  color: 'purple',
};

const MOCK_ISSUE: Issue = {
  id: '1',
  number: 123,
  title: 'Test Issue Title',
  url: 'https://github.com/test/issue/123',
  state: GitHubStates.OPEN,
  points: 5,
};

describe('IssueBarComponent', () => {
  let component: IssueBarComponent;
  let fixture: ComponentFixture<IssueBarComponent>;
  let mockTooltipService: jasmine.SpyObj<TooltipService>;

  beforeEach(async () => {
    mockTooltipService = jasmine.createSpyObj('TooltipService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [IssueBarComponent],
      providers: [
        { provide: TooltipService, useValue: mockTooltipService },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IssueBarComponent);
    component = fixture.componentInstance;

    component.issue = MOCK_ISSUE;
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Styling Logic', () => {
    it('should apply CLOSED_STYLE for a closed issue', () => {
      component.issue = { ...MOCK_ISSUE, state: GitHubStates.CLOSED };
      fixture.detectChanges();
      const closedStyle = (component as any).CLOSED_STYLE;

      expect(component.priorityStyle).toEqual(closedStyle);
    });

    it('should apply OPEN_STYLE for an open issue without a priority color', () => {
      component.issue = { ...MOCK_ISSUE, issuePriority: undefined };
      fixture.detectChanges();
      const openStyle = (component as any).OPEN_STYLE;

      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should apply OPEN_STYLE for an open issue with an invalid priority color', () => {
      const priorityWithInvalidColor: IssuePriority = { id: 'p1', name: 'High', color: 'invalid', description: '' };
      component.issue = { ...MOCK_ISSUE, issuePriority: priorityWithInvalidColor };
      fixture.detectChanges();
      const openStyle = (component as any).OPEN_STYLE;

      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should generate custom styles for an open issue with a valid priority color', () => {
      const priorityWithValidColor: IssuePriority = { id: 'p1', name: 'High', color: 'ff0000', description: '' };
      component.issue = { ...MOCK_ISSUE, issuePriority: priorityWithValidColor };
      fixture.detectChanges();

      const expectedStyle = {
        'background-color': '#ff000025',
        color: '#ff0000',
        'border-color': '#ff0000',
      };

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should apply On hold state style when issue has On hold state', () => {
      const onHoldState: IssueState = { id: 's1', name: 'On hold', color: 'red', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: onHoldState };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['On hold'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should apply Todo state style when issue has Todo state', () => {
      const todoState: IssueState = { id: 's2', name: 'Todo', color: 'yellow', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: todoState };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['Todo'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should apply In Progress state style when issue has In Progress state', () => {
      const inProgressState: IssueState = { id: 's3', name: 'In Progress', color: 'blue', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: inProgressState };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['In Progress'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should apply Review state style when issue has Review state', () => {
      const reviewState: IssueState = { id: 's4', name: 'Review', color: 'green', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: reviewState };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['Review'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should apply Done state style when issue has Done state', () => {
      const doneState: IssueState = { id: 's5', name: 'Done', color: 'gray', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: doneState };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['Done'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should fall back to CLOSED_STYLE when issue is closed and has unknown state', () => {
      const unknownState: IssueState = { id: 's6', name: 'Unknown State', color: 'gray', description: '' };
      component.issue = { ...MOCK_ISSUE, state: GitHubStates.CLOSED, issueState: unknownState };
      fixture.detectChanges();
      const closedStyle = (component as any).CLOSED_STYLE;

      expect(component.priorityStyle).toEqual(closedStyle);
    });

    it('should fall back to priority color when issue has unknown state and valid priority', () => {
      const unknownState: IssueState = { id: 's6', name: 'Unknown State', color: 'gray', description: '' };
      const priorityWithValidColor: IssuePriority = { id: 'p1', name: 'High', color: 'ff0000', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: unknownState, issuePriority: priorityWithValidColor };
      fixture.detectChanges();

      const expectedStyle = {
        'background-color': '#ff000025',
        color: '#ff0000',
        'border-color': '#ff0000',
      };

      expect(component.priorityStyle).toEqual(expectedStyle);
    });

    it('should prioritize issue state over priority color when both are present', () => {
      const inProgressState: IssueState = { id: 's3', name: 'In Progress', color: 'blue', description: '' };
      const priorityWithValidColor: IssuePriority = { id: 'p1', name: 'High', color: 'ff0000', description: '' };
      component.issue = { ...MOCK_ISSUE, issueState: inProgressState, issuePriority: priorityWithValidColor };
      fixture.detectChanges();
      const expectedStyle = (component as any).ISSUE_STATE_STYLES['In Progress'];

      expect(component.priorityStyle).toEqual(expectedStyle);
    });
  });

  describe('Epic Sub-Issue State Gradient', () => {
    it('should create gradient when epic has sub-issues with different states', () => {
      const doneState: IssueState = { id: 's1', name: 'Done', color: 'gray', description: '' };
      const inProgressState: IssueState = { id: 's2', name: 'In Progress', color: 'blue', description: '' };

      const subIssue1: Issue = { ...MOCK_ISSUE, id: 'sub1', issueState: doneState };
      const subIssue2: Issue = { ...MOCK_ISSUE, id: 'sub2', issueState: inProgressState };

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues: [subIssue1, subIssue2] };
      fixture.detectChanges();

      expect(component.priorityStyle['background']).toContain('linear-gradient');
      expect(component.priorityStyle['background']).toContain('#dbeafe');
      expect(component.priorityStyle['background']).toContain('#f3e8ff');
      expect(component.priorityStyle['background']).toContain('#93c5fd');
      expect(component.priorityStyle['background']).toContain('#d8b4fe');
    });

    it('should create gradient covering all 5 issue states', () => {
      const todoState: IssueState = { id: 's1', name: 'Todo', color: 'yellow', description: '' };
      const onHoldState: IssueState = { id: 's2', name: 'On hold', color: 'red', description: '' };
      const inProgressState: IssueState = { id: 's3', name: 'In Progress', color: 'blue', description: '' };
      const reviewState: IssueState = { id: 's4', name: 'Review', color: 'green', description: '' };
      const doneState: IssueState = { id: 's5', name: 'Done', color: 'gray', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: todoState },
        { ...MOCK_ISSUE, id: 'sub2', issueState: onHoldState },
        { ...MOCK_ISSUE, id: 'sub3', issueState: inProgressState },
        { ...MOCK_ISSUE, id: 'sub4', issueState: reviewState },
        { ...MOCK_ISSUE, id: 'sub5', issueState: doneState },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues };
      fixture.detectChanges();

      const gradient = component.priorityStyle['background'];

      expect(gradient).toContain('linear-gradient');
      expect(gradient).toContain('#f0fdf4');
      expect(gradient).toContain('#fee2e2');
      expect(gradient).toContain('#dbeafe');
      expect(gradient).toContain('#fefce8');
      expect(gradient).toContain('#f3e8ff');
    });

    it('should handle 50/50 split of Done and In Progress states', () => {
      const doneState: IssueState = { id: 's1', name: 'Done', color: 'gray', description: '' };
      const inProgressState: IssueState = { id: 's2', name: 'In Progress', color: 'blue', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub2', issueState: inProgressState },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues };
      fixture.detectChanges();

      const gradient = component.priorityStyle['background'];

      expect(gradient).toContain('linear-gradient');
      expect(gradient).toContain('50%');
    });

    it('should use dominant state for text color', () => {
      const doneState: IssueState = { id: 's1', name: 'Done', color: 'gray', description: '' };
      const inProgressState: IssueState = { id: 's2', name: 'In Progress', color: 'blue', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub2', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub3', issueState: inProgressState },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues };
      fixture.detectChanges();

      expect(component.priorityStyle['color']).toBe('#581c87');
      expect(component.priorityStyle['background']).toContain('#d8b4fe');
    });

    it('should handle sub-issues without issue state falling back to GitHub state', () => {
      const doneState: IssueState = { id: 's1', name: 'Done', color: 'gray', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub2', state: GitHubStates.CLOSED, issueState: undefined },
        { ...MOCK_ISSUE, id: 'sub3', state: GitHubStates.OPEN, issueState: undefined },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues };
      fixture.detectChanges();

      const gradient = component.priorityStyle['background'];

      expect(gradient).toContain('linear-gradient');
      expect(gradient).toContain('#f3e8ff');
      expect(gradient).toContain('#f3e8ff');
      expect(gradient).toContain('#f0fdf4');
    });

    it('should create single color when all sub-issues have same state', () => {
      const doneState: IssueState = { id: 's1', name: 'Done', color: 'gray', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub2', issueState: doneState },
        { ...MOCK_ISSUE, id: 'sub3', issueState: doneState },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, subIssues };
      fixture.detectChanges();

      const gradient = component.priorityStyle['background'];

      expect(gradient).toContain('linear-gradient');
      expect(gradient).toContain('#f3e8ff');
    });

    it('should prioritize sub-issue gradient over epic own state', () => {
      const epicState: IssueState = { id: 's1', name: 'Todo', color: 'yellow', description: '' };
      const subIssue1State: IssueState = { id: 's2', name: 'Done', color: 'gray', description: '' };
      const subIssue2State: IssueState = { id: 's3', name: 'In Progress', color: 'blue', description: '' };

      const subIssues: Issue[] = [
        { ...MOCK_ISSUE, id: 'sub1', issueState: subIssue1State },
        { ...MOCK_ISSUE, id: 'sub2', issueState: subIssue2State },
      ];

      component.issue = { ...MOCK_ISSUE, issueType: EPIC_TYPE, issueState: epicState, subIssues };
      fixture.detectChanges();

      const gradient = component.priorityStyle['background'];

      expect(gradient).toContain('linear-gradient');
      expect(gradient).toContain('#f3e8ff');
      expect(gradient).toContain('#dbeafe');
    });
  });

  describe('Tooltip Interaction', () => {
    it('should call TooltipService.show on mouseenter', () => {
      fixture.detectChanges();
      const issueLink = fixture.debugElement.query(By.css('.issue-bar'));
      issueLink.triggerEventHandler('mouseenter', null);

      expect(mockTooltipService.show).toHaveBeenCalledWith(issueLink.nativeElement, component.issue);
    });

    it('should call TooltipService.hide on mouseleave', () => {
      fixture.detectChanges();
      const issueLink = fixture.debugElement.query(By.css('.issue-bar'));
      issueLink.triggerEventHandler('mouseleave', null);

      expect(mockTooltipService.hide).toHaveBeenCalledWith();
    });
  });

  describe('Template Rendering', () => {
    it('should render the issue number', () => {
      fixture.detectChanges();
      const numberElement = fixture.debugElement.query(By.css('.number')).nativeElement;

      expect(numberElement.textContent).toContain('123');
    });

    it('should set the href attribute on the anchor tag', () => {
      fixture.detectChanges();
      const anchorElement = fixture.debugElement.query(By.css('.issue-bar')).nativeElement as HTMLAnchorElement;

      expect(anchorElement.href).toBe(MOCK_ISSUE.url);
    });

    it('should apply the calculated position style to the host element', () => {
      component.issueStyle = { left: '10%', width: '5%' };
      fixture.detectChanges();
      const anchorElement = fixture.debugElement.query(By.css('.issue-bar')).nativeElement as HTMLAnchorElement;

      expect(anchorElement.style.left).toBe('10%');
      expect(anchorElement.style.width).toBe('5%');
    });
  });
});
