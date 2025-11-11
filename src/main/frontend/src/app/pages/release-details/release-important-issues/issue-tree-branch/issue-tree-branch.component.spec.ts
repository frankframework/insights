import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IssueTreeBranchComponent } from './issue-tree-branch.component';
import { Issue } from '../../../../services/issue.service';
import { GitHubStates } from '../../../../app.service';
import { IssueTypeTagComponent } from '../../../../components/issue-type-tag/issue-type-tag.component';
import { By } from '@angular/platform-browser';

const MOCK_ISSUE: Issue = {
  id: 'PROJ-1',
  number: 1,
  title: 'This is a parent issue',
  state: GitHubStates.CLOSED,
  url: 'http://localhost/PROJ-1',
  subIssues: [
    {
      id: 'PROJ-2',
      number: 2,
      title: 'This is a sub-issue',
      state: GitHubStates.CLOSED,
      url: 'http://localhost/PROJ-2',
    },
  ],
  issueType: {
    id: 'it-3',
    name: 'Story',
    description: 'test',
    color: '#a2eeef',
  },
};

describe('IssueTreeBranchComponent', () => {
  let component: IssueTreeBranchComponent;
  let fixture: ComponentFixture<IssueTreeBranchComponent>;
  let nativeElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueTreeBranchComponent, IssueTypeTagComponent], // Import standalone components
    }).compileComponents();

    fixture = TestBed.createComponent(IssueTreeBranchComponent);
    component = fixture.componentInstance;
    nativeElement = fixture.nativeElement;
  });

  it('should create', () => {
    component.issue = MOCK_ISSUE;
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Default State & Rendering', () => {
    it('should display issue number, title, and url', () => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();

      const linkElement = nativeElement.querySelector('.issue-row') as HTMLAnchorElement;

      const numberElement = nativeElement.querySelector('.issue-number') as HTMLSpanElement;
      const titleElement = nativeElement.querySelector('.issue-title') as HTMLSpanElement;

      expect(numberElement.textContent).toContain('#1');
      expect(linkElement.href).toBe('http://localhost/PROJ-1');
      expect(titleElement.textContent).toBe('This is a parent issue');
    });

    it('should show the fold icon if sub-issues exist', () => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
      const foldIcon = nativeElement.querySelector('.issue-fold-icon');

      expect(foldIcon).toBeTruthy();
    });

    it('should render the IssueTypeTagComponent when issueType is provided', () => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
      const tagComponent = fixture.debugElement.query(By.directive(IssueTypeTagComponent));

      expect(tagComponent).toBeTruthy();
      expect(tagComponent.componentInstance.issueType).toEqual(MOCK_ISSUE.issueType);
    });

    it('should not render the IssueTypeTagComponent when not provided', () => {
      component.issue = { ...MOCK_ISSUE, issueType: undefined };
      fixture.detectChanges();
      const tagComponent = fixture.debugElement.query(By.directive(IssueTypeTagComponent));

      expect(tagComponent).toBeFalsy();
    });
  });

  describe('Expand/Collapse Logic', () => {
    beforeEach(() => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
    });

    it('should start as collapsed by default', () => {
      expect(component['expanded']).toBeFalse();
      expect(nativeElement.querySelector('.issue-tree-branch.expanded')).toBeFalsy();
    });

    it('should toggle expanded state on click', () => {
      const foldButton = nativeElement.querySelector('.issue-fold-icon') as HTMLButtonElement;

      foldButton.click();
      fixture.detectChanges();

      expect(component['expanded']).toBeTrue();

      foldButton.click();
      fixture.detectChanges();

      expect(component['expanded']).toBeFalse();
    });

    it('should not render sub-issues when collapsed', () => {
      component['expanded'] = false;
      fixture.detectChanges();
      const subIssues = nativeElement.querySelectorAll('app-issue-tree-branch');

      expect(subIssues.length).toBe(0);
    });

    it('should render sub-issues when expanded', () => {
      component['expanded'] = true;
      fixture.detectChanges();
      const subIssues = nativeElement.querySelectorAll('app-issue-tree-branch');

      expect(subIssues.length).toBe(1);
    });
  });
});
