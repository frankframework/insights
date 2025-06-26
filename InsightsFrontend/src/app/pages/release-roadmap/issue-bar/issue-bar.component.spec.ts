import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IssueBarComponent } from './issue-bar.component';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';

const mockBaseIssue: Omit<Issue, 'state' | 'issuePriority' | 'repo' | 'owner' | 'createdAt'> = {
  id: '1',
  number: 123,
  title: 'Test Issue Title',
  url: 'https://github.com/test/issue/123',
  points: 5,
};

const mockClosedIssue: Issue = {
  ...mockBaseIssue,
  state: GitHubStates.CLOSED,
} as Issue;

const mockOpenIssueWithPriority: Issue = {
  ...mockBaseIssue,
  state: GitHubStates.OPEN,
  issuePriority: {
    id: 'p1',
    name: 'High',
    color: 'ff0000',
    description: 'This is a high priority issue',
  },
} as Issue;

const mockOpenIssueWithoutPriority: Issue = {
  ...mockBaseIssue,
  points: 0,
  state: GitHubStates.OPEN,
} as Issue;

const mockOpenIssueWithInvalidColor: Issue = {
  ...mockBaseIssue,
  state: GitHubStates.OPEN,
  issuePriority: {
    id: 'p2',
    name: 'Invalid',
    color: 'invalid-hex',
    description: 'This is an issue with an invalid hex color',
  },
} as Issue;

describe('IssueBarComponent', () => {
  let component: IssueBarComponent;
  let fixture: ComponentFixture<IssueBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(IssueBarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    component.issue = mockOpenIssueWithoutPriority;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Initialization and Style Logic', () => {
    it('should set isClosed to true for a closed issue', () => {
      component.issue = mockClosedIssue;
      fixture.detectChanges();
      expect(component.isClosed).toBeTrue();
    });

    it('should apply CLOSED_STYLE for a closed issue', () => {
      component.issue = mockClosedIssue;
      fixture.detectChanges();
      const closedStyle = component['CLOSED_STYLE'];
      expect(component.priorityStyle).toEqual(closedStyle);
    });

    it('should apply OPEN_STYLE for an open issue without a priority', () => {
      component.issue = mockOpenIssueWithoutPriority;
      fixture.detectChanges();
      const openStyle = component['OPEN_STYLE'];
      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should apply OPEN_STYLE for an open issue with an invalid hex color', () => {
      component.issue = mockOpenIssueWithInvalidColor;
      fixture.detectChanges();
      const openStyle = component['OPEN_STYLE'];
      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should apply priority-specific styles for an open issue with a valid priority color', () => {
      component.issue = mockOpenIssueWithPriority;
      fixture.detectChanges();

      const expectedColor = `#${mockOpenIssueWithPriority.issuePriority!.color}`;
      const expectedStyle = {
        'background-color': `${expectedColor}25`,
        color: expectedColor,
        'border-color': expectedColor,
      };

      expect(component.priorityStyle).toEqual(expectedStyle);
    });
  });

  describe('Template Rendering', () => {
    let nativeElement: HTMLElement;

    beforeEach(() => {
      nativeElement = fixture.nativeElement;
    });

    it('should render the issue number correctly', () => {
      component.issue = mockBaseIssue as Issue;
      fixture.detectChanges();
      const numberElement = nativeElement.querySelector('.number');
      expect(numberElement?.textContent).toContain(mockBaseIssue.number.toString());
    });

    it('should set the href and title attributes on the anchor tag', () => {
      component.issue = mockBaseIssue as Issue;
      fixture.detectChanges();
      const anchorElement = nativeElement.querySelector('.issue-bar');
      expect(anchorElement?.getAttribute('href')).toBe(mockBaseIssue.url);
      expect(anchorElement?.getAttribute('title')).toBe(mockBaseIssue.title);
    });

    it('should display the title in the tooltip', () => {
      component.issue = mockBaseIssue as Issue;
      fixture.detectChanges();
      const tooltipTitleElement = nativeElement.querySelector('.tooltip-title');
      expect(tooltipTitleElement?.textContent).toContain(mockBaseIssue.title);
    });

    it('should display priority and points in the tooltip when they exist', () => {
      component.issue = mockOpenIssueWithPriority;
      fixture.detectChanges();

      const details = nativeElement.querySelectorAll('.tooltip-detail');
      expect(details.length).toBe(2);
      expect(details[0].textContent).toContain(`Priority: ${mockOpenIssueWithPriority.issuePriority!.name}`);
      expect(details[1].textContent).toContain(`Points: ${mockOpenIssueWithPriority.points}`);
    });

    it('should NOT display priority or points in the tooltip when they do not exist', () => {
      component.issue = mockOpenIssueWithoutPriority;
      fixture.detectChanges();

      const priorityElement = nativeElement.querySelector('.tooltip-detail strong');
      expect(priorityElement).toBeNull();
    });
  });
});
