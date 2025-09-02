import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FutureEpic } from './future-epic';
import { Issue } from '../../../../services/issue.service';
import { GitHubStates } from '../../../../app.service';
import { By } from '@angular/platform-browser';
import { IssueTypeTagComponent } from '../../../../components/issue-type-tag/issue-type-tag.component';

const MOCK_EPIC: Issue = {
  id: 'EPIC-1',
  number: 123,
  title: 'This is a future epic',
  state: GitHubStates.OPEN,
  url: 'http://localhost/EPIC-1',
  issueType: {
    id: 'it-epic',
    name: 'Epic',
    color: '#f0ad4e',
    description: "an epic issue type",
  },
};

describe('FutureEpic', () => {
  let component: FutureEpic;
  let fixture: ComponentFixture<FutureEpic>;
  let nativeElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FutureEpic, IssueTypeTagComponent], // Import standalone component and its dependency
    }).compileComponents();

    fixture = TestBed.createComponent(FutureEpic);
    component = fixture.componentInstance;
    nativeElement = fixture.nativeElement;
  });

  it('should create', () => {
    component.futureEpicIssue = MOCK_EPIC;
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should display the epic title, number, and link correctly', () => {
    component.futureEpicIssue = MOCK_EPIC;
    fixture.detectChanges();

    const linkElement = nativeElement.querySelector('.future-epic-card') as HTMLAnchorElement;
    const titleElement = nativeElement.querySelector('.epic-title');
    const numberElement = nativeElement.querySelector('.epic-number');

    expect(linkElement.href).toBe('http://localhost/EPIC-1');
    expect(titleElement?.textContent).toContain('This is a future epic');
    expect(numberElement?.textContent).toContain('#123');
  });

  it('should render the IssueTypeTagComponent when issueType is provided', () => {
    component.futureEpicIssue = MOCK_EPIC;
    fixture.detectChanges();

    const tagComponent = fixture.debugElement.query(By.directive(IssueTypeTagComponent));

    expect(tagComponent).toBeTruthy();
    expect(tagComponent.componentInstance.issueType).toEqual(MOCK_EPIC.issueType);
  });

  it('should not render the IssueTypeTagComponent if issueType is not provided', () => {
    component.futureEpicIssue = { ...MOCK_EPIC, issueType: undefined };
    fixture.detectChanges();

    const tagComponent = fixture.debugElement.query(By.directive(IssueTypeTagComponent));

    expect(tagComponent).toBeFalsy();
  });
});
