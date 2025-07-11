import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IssueTreeBranchComponent } from './issue-tree-branch.component';
import { ReleaseOffCanvasComponent } from '../../release-off-canvas.component';
import { Issue } from '../../../../../services/issue.service';
import { GitHubStates } from '../../../../../app.service';

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
    id: 'story-id',
    name: 'Story',
    color: 'lightgreen',
    description: 'A user story',
  },
};

class MockReleaseOffCanvasComponent {
  colorNameToRgba(color: string): string {
    switch (color) {
      case 'lightyellow': {
        return 'rgba(255, 255, 224, 1)';
      }
      case 'darkblue': {
        return 'rgba(0, 0, 139, 1)';
      }
      default: {
        return 'rgba(0, 0, 0, 1)';
      }
    }
  }
}

describe('IssueTreeBranchComponent', () => {
  let component: IssueTreeBranchComponent;
  let fixture: ComponentFixture<IssueTreeBranchComponent>;
  let nativeElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueTreeBranchComponent],
      providers: [{ provide: ReleaseOffCanvasComponent, useClass: MockReleaseOffCanvasComponent }],
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

      const linkElement = nativeElement.querySelector('.issue-number') as HTMLAnchorElement;
      const titleElement = nativeElement.querySelector('.issue-title') as HTMLSpanElement;

      expect(linkElement.textContent).toContain('#1');
      expect(linkElement.href).toBe('http://localhost/PROJ-1');
      expect(titleElement.textContent).toBe('This is a parent issue');
    });

    it('should not show the fold icon if there are no sub-issues', () => {
      component.issue = { ...MOCK_ISSUE, subIssues: [] };
      fixture.detectChanges();
      const foldIcon = nativeElement.querySelector('.issue-fold-icon');

      expect(foldIcon).toBeFalsy();
    });

    it('should show the fold icon if sub-issues exist', () => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
      const foldIcon = nativeElement.querySelector('.issue-fold-icon');

      expect(foldIcon).toBeTruthy();
    });

    it('should display the issue type when provided', () => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
      const typeElement = nativeElement.querySelector('.issue-type') as HTMLSpanElement;

      expect(typeElement).toBeTruthy();
      expect(typeElement.textContent?.trim()).toBe('Story');
      expect(typeElement.style.backgroundColor).toBe('lightgreen');
    });

    it('should not display the issue type when not provided', () => {
      component.issue = { ...MOCK_ISSUE, issueType: undefined };
      fixture.detectChanges();
      const typeElement = nativeElement.querySelector('.issue-type');

      expect(typeElement).toBeFalsy();
    });
  });

  describe('Indentation Logic (getIndent)', () => {
    it('should have 0rem indent at depth 0', () => {
      component.depth = 0;

      expect(component.getIndent()).toBe('0rem');
    });

    it('should have 3rem indent at depth 3', () => {
      component.depth = 3;

      expect(component.getIndent()).toBe('3rem');
    });

    it('should cap the indent at the MAX_SUB_ISSUE_DEPTH', () => {
      component.depth = 10;
      // @ts-ignore: Accessing private static property for test
      const maxDepth = IssueTreeBranchComponent.MAX_SUB_ISSUE_DEPTH;

      expect(component.getIndent()).toBe(`${maxDepth}rem`);
    });
  });

  describe('Expand/Collapse Logic', () => {
    beforeEach(() => {
      component.issue = MOCK_ISSUE;
      fixture.detectChanges();
    });

    it('should start as collapsed by default', () => {
      // @ts-ignore: Accessing private property for test
      expect(component.expanded).toBeFalse();
      expect(nativeElement.querySelector('.issue-tree-branch.expanded')).toBeFalsy();
    });

    it('should toggle expanded state on click', () => {
      const foldButton = nativeElement.querySelector('.issue-fold-icon') as HTMLButtonElement;

      foldButton.click();
      fixture.detectChanges();

      // @ts-ignore: Accessing private static property for test
      expect(component.expanded).toBeTrue();
      expect(foldButton.classList.contains('expanded')).toBeTrue();
      expect(foldButton.getAttribute('aria-expanded')).toBe('true');

      foldButton.click();
      fixture.detectChanges();

      // @ts-ignore: Accessing private static property for test
      expect(component.expanded).toBeFalse();
      expect(foldButton.classList.contains('expanded')).toBeFalse();
      expect(foldButton.getAttribute('aria-expanded')).toBe('false');
    });

    it('should not render sub-issues when collapsed', () => {
      // @ts-ignore: Accessing private static property for test
      component.expanded = false;
      fixture.detectChanges();
      const subIssues = nativeElement.querySelectorAll('app-issue-tree-branch');

      expect(subIssues.length).toBe(0);
    });

    it('should render sub-issues when expanded', () => {
      // @ts-ignore: Accessing private static property for test
      component.expanded = true;
      fixture.detectChanges();
      const subIssues = nativeElement.querySelectorAll('app-issue-tree-branch');

      expect(subIssues.length).toBe(1);
    });
  });

  describe('Issue Type & Text Color (getTypeTextColor)', () => {
    it('should return "white" for undefined issueType color', () => {
      expect(component.getTypeTextColor({ color: '' })).toBe('white');
    });

    it('should return "black" for a light background color', () => {
      const color = component.getTypeTextColor({ color: 'lightyellow' });

      expect(color).toBe('black');
    });

    it('should return "white" for a dark background color', () => {
      const color = component.getTypeTextColor({ color: 'darkblue' });

      expect(color).toBe('white');
    });
  });

  describe('Inputs', () => {
    it('should correctly receive issue and depth inputs', () => {
      // @ts-ignore: Accessing private static property for test
      component.issue = { id: 'T-1', number: 101, title: 'Input Test', url: '' };
      component.depth = 5;
      fixture.detectChanges();

      expect(component.issue.id).toBe('T-1');
      expect(component.depth).toBe(5);
    });
  });
});
